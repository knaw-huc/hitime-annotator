package main

import (
	"math/rand"
	"testing"
)

func TestAdd(t *testing.T) {
	s := newIntset(5)
	s.Add(1)
	s.Add(1)
	s.Add(3)
	s.Add(4)

	for _, i := range []int{1, 3, 4} {
		if !s.Contains(i) {
			t.Errorf("%d inserted but not in set", i)
		}
	}
	for _, i := range []int{0, 2, 5} {
		if s.Contains(i) {
			t.Errorf("%d in set but not inserted", i)
		}
	}
}

func TestAt(t *testing.T) {
	ints := []int{0, 2, 4, 5, 7, 9}
	m := make(map[int]struct{})
	s := newIntset(20)

	for _, i := range ints {
		m[i] = struct{}{}
		s.Add(i)
	}

	for i := 0; i < s.Len(); i++ {
		j := s.At(i)
		if _, ok := m[j]; !ok {
			t.Errorf("%d should not be set or is there twice", j)
		}
		delete(m, i) // ensure we don't see anything twice
	}
}

func TestRemove(t *testing.T) {
	ints := []int{1, 2, 3, 7, 9}
	m := make(map[int]struct{})
	s := newIntset(10)

	for _, i := range ints {
		m[i] = struct{}{}
		s.Add(i)
	}

	rand.New(rand.NewSource(42)).Shuffle(len(ints), func(i, j int) {
		ints[i], ints[j] = ints[j], ints[i]
	})

	if n := s.Len(); n != len(ints) {
		t.Errorf("expected length %d, got %d", len(ints), n)
	}

	for _, i := range ints {
		if !s.Contains(i) {
			t.Fatalf("Add failed on %d", i)
		}
		s.Remove(i)
		if s.Contains(i) {
			t.Errorf("Remove failed on %d", i)
		}
		s.Remove(i)
		if s.Contains(i) {
			t.Errorf("Remove failed on %d", i)
		}

		delete(m, i)
		for j := range m {
			if !s.Contains(j) {
				t.Errorf("set should contain %d but doesn't", j)
			}
		}
	}

	if n := s.Len(); n != 0 {
		t.Errorf("expected length 0, got %d", n)
	}
}
