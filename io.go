package main

import (
	"compress/gzip"
	"encoding/json"
	"io"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
)

func readItems(path string) ([]item, error) {
	f, err := os.Open(path)
	if err != nil {
		log.Fatal(err)
	}
	defer f.Close()

	var r io.ReadCloser = f
	if filepath.Ext(path) == ".gz" {
		r, err = gzip.NewReader(r)
		if err != nil {
			log.Fatal(err)
		}
		defer r.Close()
	}

	dec := json.NewDecoder(r)
	var items []item
	for {
		var it item
		switch err = dec.Decode(&it); err {
		case nil:
			items = append(items, it)
		case io.EOF:
			return items, nil
		default:
			return nil, err
		}
	}
}

// Writes items to path, each item JSON-encoded, one item per line.
//
// If path has the extension .gz, the JSON is gzipped first.
func writeItems(path string, items []item) error {
	// First write to temp file, then move to final destination.
	// Not doing this runs the risk of overwriting an existing
	// file with half a file due to a crash.
	tmp, err := ioutil.TempFile(filepath.Dir(path), "annotator")
	if err != nil {
		return err
	}

	defer func() {
		tmp.Close()
		if err == nil {
			err = os.Rename(tmp.Name(), path)
		}
		if err != nil {
			os.Remove(tmp.Name())
		}
	}()

	var w io.WriteCloser = tmp
	if filepath.Ext(path) == ".gz" {
		w, _ = gzip.NewWriterLevel(tmp, gzip.BestCompression)
		defer w.Close()
	}

	for i := range items {
		if err = json.NewEncoder(w).Encode(&items[i]); err != nil {
			break
		}
	}
	return err
}
