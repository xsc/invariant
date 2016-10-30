# invariant

__invariant__ is a library providing semantic invariants on Clojure data
structures. It is based on the excellent [specter][specter] library and
will integrate nicely with [clojure.spec][cljspec].

[![Build Status](https://travis-ci.org/xsc/invariant.svg?branch=master)](https://travis-ci.org/xsc/invariant)
[![Clojars Artifact](https://img.shields.io/clojars/v/invariant.svg)](https://clojars.org/invariant)

This library requires Clojure ≥ 1.7.0.

## Usage

__invariant__ is very young and the API might change, although I don't expect
there to be any breakage regarding  `invariant.core/invariant`.

### Semantic Validation

Invariants represent two passes over the data: one for collecting all
information needed to decide whether the invariant holds for a single element;
and another to actually verify those elements.

```clojure
(require '[invariant.core :as i]
         '[invariant.specter :refer [dfs]]
         '[com.rpl.specter :refer :all])

(def verify-declaration-before-usage
  (i/invariant
    {:name    :validator/variables-have-been-declared-before-usage
     :sources [:declarations ALL :name]
     :targets [:body (dfs :variable) (must :name)]
     :state   #{}
     :reduce  conj
     :verify  contains?}))
```

Now, if this is invariant is run against a piece of data, all elements matching
the [specter][specter] navigator `:sources` will be collected and used to
generate an internal verification state using `:reduce` and `:state` – in this
case, a set of all `:name` keys within `:declarations`.

Then, `:targets` is used to find elements to verify, running `:verify` on every
single one – here, we ensure that every variable name within `:body` is
contained in our previously generated set.

Let's see it in action:

```clojure
(verify-declaration-before-usage
  {:declarations [{:name "a", :type "int"}
                  {:name "b", :type "float"}]
   :body [{:function-name "+"
           :function-args [{:variable {:name "a"}}
                           {:variable {:name "b"}}]}]})
;; => nil
```

No errors! But what happens if we remove a declaration?

```clojure
(verify-declaration-before-usage
  {:declarations [{:name "a", :type "int"}]
   :body [{:function-name "+"
           :function-args [{:variable {:name "a"}}
                           {:variable {:name "b"}}]}]})
;; => (#InvariantBroken{:invariant-name :validator/variables-have-been-declared-before-usage,
;;                      :state #{"a"},
;;                      :value "b"})
```

Great – and this might even be enough to generate a useful error message!

### Structural Validation

Note that the following passes our previous invariant without problem:

```clojure
(verify-declaration-before-usage {})
;; => nil
```

Invariants do structural validation for you, so you should pair them with
something like [clojure.spec][cljspec] or [plumatic/schema][schema].

[schema]: https://github.com/plumtic/schema
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
