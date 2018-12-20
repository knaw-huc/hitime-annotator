package main

// An intset is a set of integers.
type intset struct {
	// Implemented as the data structure described by Russ Cox at
	// https://research.swtch.com/sparse.
	dense, sparse []int
}

// New creates an empty intset that can hold integers up to max (exclusive).
func newIntset(max int) intset {
	elem := make([]int, max, 2*max)
	return intset{
		sparse: elem,
		dense:  elem[max:],
	}
}

func (s *intset) Init(max int) {
	elem := make([]int, max, 2*max)
	*s = intset{
		sparse: elem,
		dense:  elem[max:],
	}
}

type entry struct {
	dense, sparse int
}

func (s *intset) Add(i int) {
	if s.Contains(i) {
		return
	}
	n := s.Len()
	s.dense = append(s.dense, i)
	s.sparse[i] = n
}

// At returns the i'th member of the set, in some arbitrary order.
//
// s.At(0) through s.At(s.Len()-1) are all the elements of s.
func (s *intset) At(i int) int { return s.dense[i] }

// Contains reports whether s contains i.
func (s *intset) Contains(i int) bool {
	return i < len(s.sparse) && s.sparse[i] < s.Len() && s.dense[s.sparse[i]] == i
}

func (s *intset) Len() int { return len(s.dense) }

// Remove removes i from s if i is contained in s. It reports whether i was in s.
func (s *intset) Remove(i int) bool {
	if !s.Contains(i) {
		return false
	}

	n := s.Len()
	j := s.dense[n-1]
	s.dense[s.sparse[i]] = j
	s.sparse[j] = s.sparse[i]
	s.dense = s.dense[:n-1]

	return true
}
