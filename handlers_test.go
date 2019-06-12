package main

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"

	"github.com/stretchr/testify/assert"
)

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
