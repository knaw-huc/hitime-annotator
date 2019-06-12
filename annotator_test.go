package main

import (
	"bytes"
	"compress/gzip"
	"io/ioutil"
	"os"
	"path/filepath"
	"reflect"
	"strconv"
	"sync"
	"testing"
)

func TestReadWriteItems(t *testing.T) {
	occs := []occurrence{
		{Input: "Foo", Candidates: []candidate{
			{Id: "1", Names: []string{"Foo", "foo"}},
		}},
		{Input: "Bar", Candidates: []candidate{
			{Id: "2", Names: []string{"Bar", "bar"}},
		}},
	}
	const occsjson = `
		{"input": "Foo", "candidates": [{"id": "1", "names": ["Foo", "foo"]}]}
		{"input": "Bar", "candidates": [{"id": "2", "names": ["Bar", "bar"]}]}
	`

	dir, err := ioutil.TempDir("", "annotator_test")
	if err != nil {
		t.Fatal(err)
	}
	defer os.RemoveAll(dir)

	// Read.
	for _, ext := range []string{"", ".gz"} {
		content := []byte(occsjson)
		if ext == ".gz" {
			content = compress(content)
		}

		path := filepath.Join(dir, "occs1"+ext)
		if err := ioutil.WriteFile(path, content, 0600); err != nil {
			t.Fatal(err)
		}

		readAndCheckEquals(t, path, occs)
	}

	// Write then read.
	for _, ext := range []string{"", ".gz"} {
		path := filepath.Join(dir, "occs2"+ext)
		if err := writeFile(path, occs); err != nil {
			t.Fatal(err)
		}
		readAndCheckEquals(t, path, occs)
	}
}

func readAndCheckEquals(t *testing.T, path string, occs []occurrence) {
	t.Helper()
	got, err := readFile(path)
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(got, occs) {
		t.Errorf("got %v, wanted %v", got, occs)
	}
}

func compress(p []byte) []byte {
	buf := new(bytes.Buffer)
	w := gzip.NewWriter(buf)
	w.Write(p)
	w.Close()
	return buf.Bytes()
}

func TestSetGolden(t *testing.T) {
	occs := make([]occurrence, 10000)
	a := &annotator{occs: occs}
	a.initTodo()

	const nproc = 10
	batchsize := len(occs) / nproc
	var wg sync.WaitGroup

	wg.Add(nproc)
	for i := 0; i < nproc; i++ {
		go func(start int) {
			t.Helper()

			for j := start; j < start+batchsize; j++ {
				done, err := a.setGolden(j, strconv.Itoa(j))
				if err != nil {
					t.Fatal(err)
				}
				if done < 0 || done > len(occs) {
					t.Errorf("invalid number done %d", done)
				}
			}

			wg.Done()
		}(i * batchsize)
	}
	wg.Wait()

	for i := 222; i < 234; i++ {
		done, err := a.setGolden(i, strconv.Itoa(i))
		if err == nil || err.Error() != "already answered" {
			t.Fatalf("unexpected error: %v", err)
		}
		if done != len(occs) {
			t.Fatalf("expected everything done, got %d out of %d", done, len(occs))
		}
	}
}
