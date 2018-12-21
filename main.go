package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
)

var debug = false

func init() {
	flag.BoolVar(&debug, "debug", false, "debug mode")
}

func main() {
	flag.Parse()
	args := flag.Args()
	if len(args) != 2 {
		fmt.Fprintf(os.Stderr, "usage: %s addr file\n", os.Args[0])
		os.Exit(1)
	}

	a, err := newAnnotator(args[1])
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("%d items", len(a.items))

	srv := &http.Server{
		Addr:    args[0],
		Handler: a.makeHandler(),
	}

	// idleClosed is closed after srv.Shutdown has returned.
	// At that point, all idle connections have been closed and we can
	// terminate the program.
	idleClosed := make(chan struct{})

	// Catch SIGINT (ctrl+c) and gracefully shutdown the server, so that we
	// can save what we have.
	go func() {
		sigs := make(chan os.Signal, 1)
		signal.Notify(sigs, os.Interrupt)
		<-sigs

		if err := srv.Shutdown(context.Background()); err != nil {
			log.Printf("HTTP server Shutdown: %v", err)
		}
		close(idleClosed)
	}()

	if err := srv.ListenAndServe(); err != http.ErrServerClosed {
		log.Printf("HTTP server ListenAndServe: %v", err)
	}

	<-idleClosed
	err = a.saveLocked()
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
