package main

import (
	"errors"
	"fmt"
	"html/template"
	"log"
	"math/rand"
	"net/http"
	"strconv"
	"sync"

	"github.com/gin-gonic/gin"
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
	if !debug {
		gin.SetMode(gin.ReleaseMode)
	}

	r := gin.Default()
	r.SetHTMLTemplate(html)

	r.GET("/", a.home)
	r.GET("/annotate/", a.annotateRandom)
	r.GET("/annotate/:index/", a.annotate)
	r.POST("/annotate/:index/save", a.postAnswer)
	r.GET("/dump/", a.dump)
	r.GET("/save/", func(c *gin.Context) { a.save(c) })

	return r
}

func (a *annotator) dump(c *gin.Context) {
	a.mu.RLock()
	defer a.mu.RUnlock()

	c.JSON(http.StatusOK, a.items)
}

func (a *annotator) save(c *gin.Context) {
	err := a.saveLocked()
	switch err {
	case nil:
		c.String(http.StatusOK, "Successfully saved to %q", a.path)
	default:
		c.String(http.StatusInternalServerError,
			"Error saving to %q: %v", a.path, err)
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
func (a *annotator) getIndex(c *gin.Context) int {
	idxparam := c.Param("index")
	i, err := strconv.Atoi(idxparam)

	if err != nil || i < 0 || i >= len(a.items) {
		c.String(http.StatusNotFound, "invalid index %q", idxparam)
		return -1
	}
	return i
}

func (a *annotator) home(c *gin.Context) {
	a.mu.RLock()
	numTodo := a.todo.Len()
	a.mu.RUnlock()

	c.HTML(http.StatusOK, "home", struct{ Todo, Total int }{
		Todo:  numTodo,
		Total: len(a.items),
	})
}

func init() {
	// Wants a struct{ Todo, Total int } as argument.
	template.Must(html.New("home").Parse(`<html>
<head><title>Annotator</title></head>
<body>
	<div>To do: {{ .Todo }} items out of {{ .Total }} left to annotate.</div>
	<div><a href="/annotate/">Annotate random</a></div>
</body>
</html>`))
}

// Renders the annotation interface for a given index.
func (a *annotator) annotate(c *gin.Context) {
	i := a.getIndex(c)
	if i == -1 {
		return
	}

	c.HTML(http.StatusOK, "annotate", a.items[i])
}

func init() {
	// Wants an item as argument.
	template.Must(html.New("annotate").Parse(`<html>
<head><title>Annotator</title></head>
<body>
	<div>Input: <strong>{{ .Input }}</strong></div>
	<form action="save" method="post">
	{{ range .Candidates }}
		<div>
			<input type="radio" name="answer" value="{{ .Id }}">
				{{ range .Names }}<em>{{ . }}</em>, {{ end }}
				<small>(distance {{ .Distance }})</small>
			</input>
		</div>
	{{ end }}
	<div>
		<input type="radio" name="answer" value="?">
		<em>Not in list</em>
		</input>
	</div>
	<input type="submit" value="Save"/>
	</form>

	<div><a href="..">Skip</a> (random other item)</div>
	<div><a href="/">Home</a>
</body>
</html>`))
}

// Redirects to the annotation page for a random unannotated item.
func (a *annotator) annotateRandom(c *gin.Context) {
	i := a.todo.At(rand.Intn(a.todo.Len()))
	c.Redirect(http.StatusTemporaryRedirect, fmt.Sprintf("/annotate/%d/", i))
}

func (a *annotator) postAnswer(c *gin.Context) {
	i := a.getIndex(c)
	if i == -1 {
		return
	}

	answer := c.PostForm("answer")
	if answer == "" {
		c.String(http.StatusBadRequest, "No answer in POST data")
		return
	}

	done, err := a.setGolden(i, answer)
	if err != nil {
		c.String(http.StatusInternalServerError, err.Error())
		return
	}

	c.HTML(http.StatusOK, "postanswer", struct {
		Answer      string
		Done, Total int
	}{
		Answer: answer,
		Done:   done,
		Total:  len(a.items),
	})
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

func init() {
	// Wants a struct{Answer string; Done, Total int} as argument.
	template.Must(html.New("postanswer").Parse(`<html>
<head><title>Save</title></head>
<body>
	<div>Successfully saved answer: <strong>{{ .Answer }}.</div>
	<div>{{ .Done }} out of {{ .Total }} done.</div>
	<div><a href="..">Continue</a></div>
</body>
</html>`))
}
