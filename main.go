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
	log.Printf("%d occurrences", len(a.occs))

	srv := &http.Server{
		Addr:    os.Args[1],
		Handler: a.makeHandler(),
	}

	go a.savePeriodically()

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
