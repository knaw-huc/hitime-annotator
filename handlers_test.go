package main

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestClampRange(t *testing.T) {
	from, upto := clampRange(3, 2, 100)
	assert.Equal(t, 3, from)
	assert.Equal(t, 5, upto)

	from, upto = clampRange(4, 6, 8)
	assert.Equal(t, 4, from)
	assert.Equal(t, 8, upto)

	from, upto = clampRange(0, 0, 0)
	assert.Equal(t, 0, from)
	assert.Equal(t, 0, upto)
}

func TestUintValue(t *testing.T) {
	w := httptest.NewRecorder()
	i, err := uintValue(w, url.Values{"key": []string{"42"}}, "key", 1)
	assert.Equal(t, 42, i)
	assert.Equal(t, http.StatusOK, w.Code)
	assert.Nil(t, err)

	w = httptest.NewRecorder()
	i, err = uintValue(w, url.Values{}, "key", 1)
	assert.Equal(t, 1, i)
	assert.Equal(t, http.StatusOK, w.Code)
	assert.Nil(t, err)

	w = httptest.NewRecorder()
	i, err = uintValue(w, url.Values{"key": []string{"-1"}}, "key", 1)
	assert.Equal(t, http.StatusBadRequest, w.Code)
	assert.NotNil(t, err)
	assert.Contains(t, w.Body.String(), "invalid")
}

func TestGetTerm(t *testing.T) {
	a := &annotator{}
	r := a.makeHandler()

	req := httptest.NewRequest("GET", "/api/term", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	assert.Equal(t, http.StatusBadRequest, w.Code)
	for _, hint := range []string{"missing", "parameter", "term"} {
		assert.Contains(t, w.Body.String(), hint)
	}

	req = httptest.NewRequest("GET", "/api/term?term=some_key", nil)
	w = httptest.NewRecorder()
	r.ServeHTTP(w, req)
	assert.Equal(t, http.StatusNotFound, w.Code) // test uses empty db => NotFound
}

func TestGetTerms(t *testing.T) {
	a := &annotator{}
	r := a.makeHandler()

	req := httptest.NewRequest("GET", "/api/terms?term=some_key", nil)
	w := httptest.NewRecorder()
	r.ServeHTTP(w, req)
	assert.Equal(t, http.StatusBadRequest, w.Code)
	for _, hint := range []string{"superfluous", "parameter", "term"} {
		assert.Contains(t, w.Body.String(), hint)
	}
}
