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
	items := []item{
		{Input: "Foo", Candidates: []candidate{
			{Id: "1", Names: []string{"Foo", "foo"}},
		}},
		{Input: "Bar", Candidates: []candidate{
			{Id: "2", Names: []string{"Bar", "bar"}},
		}},
	}
	const itemsjson = `
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
		content := []byte(itemsjson)
		if ext == ".gz" {
			content = compress(content)
		}

		path := filepath.Join(dir, "items1"+ext)
		if err := ioutil.WriteFile(path, content, 0600); err != nil {
			t.Fatal(err)
		}

		readAndCheckEquals(t, path, items)
	}

	// Write then read.
	for _, ext := range []string{"", ".gz"} {
		path := filepath.Join(dir, "items2"+ext)
		if err := writeItems(path, items); err != nil {
			t.Fatal(err)
		}
		readAndCheckEquals(t, path, items)
	}
}

func readAndCheckEquals(t *testing.T, path string, items []item) {
	t.Helper()
	got, err := readItems(path)
	if err != nil {
		t.Fatal(err)
	}
	if !reflect.DeepEqual(got, items) {
		t.Errorf("got %v, wanted %v", got, items)
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
	items := make([]item, 10000)
	a := newAnnotator(items)

	const nproc = 10
	batchsize := len(items) / nproc
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
				if done < 0 || done > len(items) {
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
		if done != len(items) {
			t.Fatalf("expected everything done, got %d out of %d", done, len(items))
		}
	}
}
