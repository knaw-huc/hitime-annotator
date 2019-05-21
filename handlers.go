package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"html/template"
	"io/ioutil"
	"log"
	"math/rand"
	"net/http"
	"net/url"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/julienschmidt/httprouter"
)

var html = template.New("")

// An annotator holds a collection of items, some annotated, some not yet.
type annotator struct {
	items   []item
	byInput map[string][]int // item indices with same input
	byFreq  []string         // input strings ordered by freq desc
	path    string           // path of file read from, or ""
	mu      sync.RWMutex     // protects todo, lastChange and lastSave
	todo    intset           // indices of items not yet annotated

	lastChange, lastSave time.Time // timestamps for periodic saving goroutine
}

type inputFreq struct {
	Key  string `json:"key"`
	Freq int    `json:"freq"`
}

func newAnnotator(path string) (a *annotator, err error) {
	items, err := readItems(path)
	if err != nil {
		return
	}
	a = &annotator{items: items, path: path}
	a.initTodo()
	a.groupByFreqDesc()

	for _, k := range a.byFreq {
		fmt.Printf("%s occurs %d times\n", k, len(a.byInput[k]))
	}

	return
}

func (a *annotator) initTodo() {
	a.todo.Init(len(a.items))

	for i, it := range a.items {
		if it.Golden == "" {
			a.todo.Add(i)
		}
	}
}

func (a *annotator) groupByFreqDesc() {
	// group all item indices by their input string
	a.byInput = make(map[string][]int, len(a.items)/2) // guestimate # of unique input strings
	for i, it := range a.items {
		a.byInput[it.Input] = append(a.byInput[it.Input], i)
	}

	fmt.Printf("Found %d unique input strings\n", len(a.byInput))

	// order all unique keys from frequency map into separate slice
	keys := make([]string, len(a.byInput))
	i := 0
	for k := range a.byInput {
		keys[i] = k
		i++
	}
	sort.Slice(keys, func(i, j int) bool {
		return len(a.byInput[keys[i]]) > len(a.byInput[keys[j]])
	})

	a.byFreq = keys
}

// Make an HTTP handler for a.
func (a *annotator) makeHandler() http.Handler {
	r := httprouter.New()

	r.Handler("GET", "/", http.RedirectHandler("/ui/", http.StatusPermanentRedirect))

	r.GET("/api/dump", a.dump)
	r.GET("/api/save", a.save)
	r.GET("/api/terms", a.listTerms)
	r.GET("/api/item/:index", a.getItem)
	r.PUT("/api/item/:index", a.putAnswer)
	r.GET("/api/randomindex", a.randomIndex)
	r.GET("/api/statistics", a.statistics)

	r.GET("/ui/*path", ui)

	return r
}

func (a *annotator) dump(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	a.mu.RLock()
	defer a.mu.RUnlock()

	writeJSON(w, a.items)
}

func (a *annotator) save(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	err := a.lockAndSave()
	switch err {
	case nil:
		w.WriteHeader(http.StatusOK)
	default:
		http.Error(w, fmt.Sprintf("Error saving to %q: %v", a.path, err),
			http.StatusInternalServerError)
	}
}

func (a *annotator) lockAndSave() error {
	a.mu.RLock()
	defer a.mu.RUnlock()
	return a.saveLocked()
}

func (a *annotator) saveLocked() error {
	log.Printf("saving to %q", a.path)
	if err := writeItems(a.path, a.items); err != nil {
		return err
	}

	a.lastSave = time.Now()
	return nil
}

func (a *annotator) savePeriodically() {
	for range time.NewTicker(5 * time.Minute).C {
		var err error

		a.mu.RLock()
		if a.lastChange.After(a.lastSave) {
			err = a.saveLocked()
		}
		a.mu.RUnlock()

		if err != nil {
			log.Print(err)
		}
	}
}

// Gets the "index" param from c as an integer and checks whether it is
// in-bounds for items. Otherwise, returns -1 after rendering an error message.
func (a *annotator) getIndex(w http.ResponseWriter, ps httprouter.Params) int {
	idxparam := ps.ByName("index")
	i, err := strconv.Atoi(idxparam)

	if err != nil || i < 0 || i >= len(a.items) {
		http.Error(w, fmt.Sprintf("invalid index %q", idxparam), http.StatusNotFound)
		return -1
	}
	return i
}

// Returns -1 on error.
func intValue(w http.ResponseWriter, v url.Values, key string, def int) int {
	s := v.Get(key)
	if s == "" {
		return def
	}
	i, err := strconv.Atoi(s)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "invalid %s parameter: %q", key, s)
		return -1
	}
	return i
}

func (a *annotator) statistics(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	a.mu.RLock()
	todo := a.todo.Len()
	total := len(a.items)
	a.mu.RUnlock()

	writeJSON(w, struct {
		Todo int `json:"todo"`
		Done int `json:"done"`
	}{
		Todo: todo,
		Done: total - todo,
	})
}

func (a *annotator) getItem(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	i := a.getIndex(w, ps)
	if i == -1 {
		return
	}

	writeJSON(w, a.items[i])
}

func (a *annotator) listTerms(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	uparams := r.URL.Query()
	from := intValue(w, uparams, "from", 0)
	if from == -1 {
		return
	}

	size := intValue(w, uparams, "size", 10)
	if size == -1 {
		return
	}

	// clamp request params to frequency mapping bounds
	upto := from + size // optimistic init
	if from >= len(a.byFreq) {
		from = len(a.byFreq) - 1 // max index
		upto = from
	} else if from+size > len(a.byFreq) {
		upto = len(a.byFreq)
	}

	keys := a.byFreq[from:upto]
	freq := make([]inputFreq, len(keys))
	for i, k := range keys {
		freq[i] = inputFreq{Key: k, Freq: len(a.byInput[k])}
	}

	writeJSON(w, freq)
}

func (a *annotator) randomIndex(w http.ResponseWriter, r *http.Request, _ httprouter.Params) {
	a.mu.RLock()
	i := rand.Intn(a.todo.Len())
	a.mu.RUnlock()

	writeJSON(w, i)
}

func (a *annotator) putAnswer(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	i := a.getIndex(w, ps)
	if i == -1 {
		return
	}

	answer, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Print(err)
		return
	}

	_, err = a.setGolden(i, string(answer))
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	w.WriteHeader(http.StatusOK)
}

// Stores the given answer for the i'th item in a.
// Reports the number of items done and an error if the i'th item was already
// annotated with an answer.
func (a *annotator) setGolden(i int, answer string) (done int, err error) {
	a.mu.Lock()
	defer a.mu.Unlock()

	done = len(a.items) - a.todo.Len()

	if !a.todo.Remove(i) {
		err = errors.New("already answered")
		return
	}

	a.items[i].Golden = answer
	a.lastChange = time.Now()
	done++
	return
}

func writeJSON(w http.ResponseWriter, x interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)

	err := json.NewEncoder(w).Encode(x)
	if err != nil {
		log.Print(err)
	}
}

// Serve UI components. Any path that does not resolve to a file inside s.uiDir
// serves index.html instead, so the React router can take care of it.
//
// Roughly equivalent to the .htaccess rules
//
//      RewriteCond %{REQUEST_FILENAME} !-f
//      RewriteRule ^ index.html [QSA,L]
func ui(w http.ResponseWriter, r *http.Request, ps httprouter.Params) {
	path := httprouter.CleanPath(ps.ByName("path"))

	if path != "/favicon.png" && !strings.HasPrefix(path, "/static/") {
		path = "/index.html"
	}
	http.ServeFile(w, r, filepath.Join("ui", path))
	return
}
