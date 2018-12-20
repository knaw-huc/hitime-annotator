package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"

	"github.com/gin-gonic/gin"
)

var debug = false

func init() {
	flag.BoolVar(&debug, "debug", false, "debug mode")
}

func main() {
	flag.Parse()
	if !debug {
		gin.SetMode(gin.ReleaseMode)
	}

	args := flag.Args()
	if len(args) != 2 {
		fmt.Fprintf(os.Stderr, "usage: %s addr file\n", os.Args[0])
		os.Exit(1)
	}

	path := args[1]
	items, err := readItems(path)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("%d items", len(items))

	addr := args[0]
	srv := &http.Server{
		Addr: addr,
	}

	// shutdownCh is closed after srv.Shutdown has returned, meaning that
	// all connections have been closed.
	shutdownCh := make(chan struct{})

	shutdown := func(ctx context.Context) {
		if err := srv.Shutdown(ctx); err != nil {
			log.Printf("HTTP server Shutdown: %v", err)
		}
		close(shutdownCh)
	}

	// Catch SIGINT (ctrl+c) and gracefully shutdown the server, so that we
	// can save what we have.
	go func() {
		sigs := make(chan os.Signal, 1)
		signal.Notify(sigs, os.Interrupt)
		<-sigs
		shutdown(context.Background())
	}()

	r := gin.Default()
	r.SetHTMLTemplate(html)

	a := newAnnotator(items)
	r.GET("/", a.home)
	r.GET("/annotate/", a.annotateRandom)
	r.GET("/annotate/:index/", a.annotate)
	r.POST("/annotate/:index/save", a.save)

	r.GET("/dump/", a.dump)
	r.GET("/save/", func(c *gin.Context) { a.dumpTo(c, path) })

	r.GET("/shutdown/", func(c *gin.Context) {
		c.HTML(http.StatusOK, "confirmShutdown", nil)
	})
	r.GET("/shutdown/really", func(c *gin.Context) { shutdown(c) })

	srv.Handler = r
	if err := srv.ListenAndServe(); err != http.ErrServerClosed {
		log.Printf("HTTP server ListenAndServe: %v", err)
	}

	<-shutdownCh
	err = writeItems(path, a.items)
	if err != nil {
		log.Fatal(err)
	}
}

// An item is an input to be annotated, its candidates, and optionally
// its gold standard.
type item struct {
	Input      string      `json:"input"`
	Candidates []candidate `json:"candidates"`

	// Golden is the id of the true answer, "" for not yet assessed, "?" for unknown.
	Golden string `json:"golden,omitempty"`
}

type candidate struct {
	Id       string   `json:"id"`
	Names    []string `json:"names"`
	Distance float64  `json:"distance"`
}
