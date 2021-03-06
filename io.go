package main

import (
	"compress/gzip"
	"encoding/json"
	"io"
	"io/ioutil"
	"os"
	"path/filepath"
)

func readFile(path string) (occs []occurrence, err error) {
	f, err := os.Open(path)
	if err != nil {
		return
	}
	defer f.Close()

	var r io.ReadCloser = f
	if filepath.Ext(path) == ".gz" {
		r, err = gzip.NewReader(r)
		if err != nil {
			return
		}
		defer r.Close()
	}

	dec := json.NewDecoder(r)
	for {
		var occ occurrence
		switch err = dec.Decode(&occ); err {
		case nil:
			occs = append(occs, occ)
		case io.EOF:
			err = nil
			return
		default:
			return
		}
	}
}

// Writes occ to path, each item JSON-encoded, one occurrence per line.
//
// If path has the extension .gz, the JSON is gzipped first.
func writeFile(path string, occ []occurrence) error {
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

	for i := range occ {
		if err = json.NewEncoder(w).Encode(&occ[i]); err != nil {
			break
		}
	}
	return err
}
