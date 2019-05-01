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
	"path/filepath"
	"strconv"
	"strings"
	"sync"

	"github.com/julienschmidt/httprouter"
)

var html = template.New("")

// An annotator holds a collection of items, some annotated, some not yet.
type annotator struct {
	items []item
	path  string       // path of file read from, or ""
	mu    sync.RWMutex // protects todo
	todo  intset       // indices of items not yet annotated
}

func newAnnotator(path string) (a *annotator, err error) {
	items, err := readItems(path)
	if err != nil {
		return
	}
	a = &annotator{items: items, path: path}
	a.initTodo()
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

// Make an HTTP handler for a.
func (a *annotator) makeHandler() http.Handler {
	r := httprouter.New()

	r.Handler("GET", "/", http.RedirectHandler("/ui/", http.StatusPermanentRedirect))

	r.GET("/dump/", a.dump)
	r.GET("/save/", a.save)

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
	err := a.saveLocked()
	switch err {
	case nil:
		fmt.Fprintf(w, "Successfully saved to %q", a.path)
	default:
		http.Error(w, fmt.Sprintf("Error saving to %q: %v", a.path, err),
			http.StatusInternalServerError)
	}
}

func (a *annotator) saveLocked() error {
	log.Printf("dumping to %q", a.path)
	a.mu.RLock()
	defer a.mu.RUnlock()
	return writeItems(a.path, a.items)
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
