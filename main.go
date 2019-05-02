package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
)

func main() {
	if len(os.Args) != 3 {
		fmt.Fprintf(os.Stderr, "usage: %s addr file\n", os.Args[0])
		os.Exit(1)
	}

	a, err := newAnnotator(os.Args[2])
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("%d items", len(a.items))

	srv := &http.Server{
		Addr:    os.Args[1],
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
	err = a.lockAndSave()
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
