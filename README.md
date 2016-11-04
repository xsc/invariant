# invariant

__[Documentation](https://xsc.github.io/invariant/)__

__invariant__ is a library providing semantic invariants on Clojure data
structures. It is based on the excellent [specter][specter] library and
will integrate nicely with [clojure.spec][cljspec].

[![Build Status](https://travis-ci.org/xsc/invariant.svg?branch=master)](https://travis-ci.org/xsc/invariant)
[![Clojars Artifact](https://img.shields.io/clojars/v/invariant.svg)](https://clojars.org/invariant)

This library requires Clojure ≥ 1.7.0.

## Usage

__invariant__ is very young and the API might change, although I don't expect
there to be any significant breakage.

```clojure
(require '[invariant.core :as invariant]
         '[com.rpl.specter :refer :all])
```

## Quickstart

Let's, for the sake of it, verify that within a vector of vectors, every
subvector's elements sum up to the same value.

```clojure
(def sums-identical?
  (-> (invariant/on [ALL])
      (invariant/as :expected-sum #(apply + (first %)))
      (invariant/each
        (invariant/property
          :matches-expected-sum?
          (fn [{:keys [expected-sum]} v]
            (= expected-sum (apply + v)))))))
```

We can now check the invariant against a piece of data, producing either
`nil` or a seq of errors.

```clojure
(invariant/check sums-identical? [[1 3] [2 2] [5 7]])
;; => (#:invariant{:name  :matches-expected-sum?,
;;                 :state {:expected-sum 4},
;;                 :path  [ALL 2],
;;                 :value [5 7]})
```

More complex invariants are possible, e.g. recursive ones:

```clojure
(def all-values-are-integers
  (invariant/recursive
    [self]
    (invariant/and
      (invariant/value :value-int? (comp integer? :value))
      (-> (invariant/on [:children ALL])
          (invariant/each self)))))
```

Or invariants describing the relationship between parts of the data:

```clojure
(def all-variables-have-been-declared
  (-> (invariant/on [:body (walker :variable) :variable (must :name)])
      (invariant/as :declared-variables [:declarations ALL :name] conj #{})
      (invariant/each
        (invariant/property
          :variable-declared?
          (fn [{:keys [declared-variables]} v]
            (contains? declared-variables v))))))
```

See the [auto-generated documentation](https://xsc.github.io/invariant/) for
more details.


[specter]: https://github.com/nathanmarz/specter
[cljspec]: http://clojure.org/guides/spec

## License

```
MIT License

Copyright (c) 2016 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
