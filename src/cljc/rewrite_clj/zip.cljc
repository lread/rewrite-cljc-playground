(ns rewrite-clj.zip
  "API for zipper navigation and updating Clojure/ClojureScript source code."
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [clojure.string :as string]
            [rewrite-clj.impl.zip.base]
            [rewrite-clj.impl.zip.move]
            [rewrite-clj.impl.zip.find]
            [rewrite-clj.impl.zip.edit]
            [rewrite-clj.impl.zip.insert]
            [rewrite-clj.impl.zip.remove]
            [rewrite-clj.impl.zip.seq]
            [rewrite-clj.impl.zip.subedit :include-macros true]
            [rewrite-clj.impl.zip.walk]
            [rewrite-clj.impl.zip.whitespace]
            [rewrite-clj.impl.custom-zipper.core :as z]
            #?(:clj [rewrite-clj.impl.potemkin2 :refer [import-vars]]))
  #?(:cljs (:require-macros [rewrite-clj.zip]
                            [rewrite-clj.impl.potemkin2-cljs :refer [import-vars]])))

;; import macros for both clj and cljs
#?(:clj
   (import-vars
    [rewrite-clj.impl.zip.subedit
     edit-> edit->>
     subedit-> subedit->>]))

(import-vars
 [rewrite-clj.impl.custom-zipper.core
  node position root]

 [rewrite-clj.impl.zip.base
  child-sexprs
  edn* edn
  tag sexpr
  length
  value
  #?(:clj of-file)
  of-string
  string root-string
  print print-root]

 [rewrite-clj.impl.zip.edit
  replace edit splice
  prefix suffix]

 [rewrite-clj.impl.zip.find
  find find-next
  find-depth-first
  find-next-depth-first
  find-tag find-next-tag
  find-value find-next-value
  find-token find-next-token
  ;; clsj extras
  find-last-by-pos
  find-tag-by-pos]

 [rewrite-clj.impl.zip.insert
  insert-right insert-left
  insert-child append-child]

 [rewrite-clj.impl.zip.move
  left right up down prev next
  leftmost rightmost
  leftmost? rightmost? end?]

 [rewrite-clj.impl.zip.remove
  remove
  ;; cljs extras
  remove-preserve-newline]

 [rewrite-clj.impl.zip.seq
  seq? list? vector? set? map?
  map map-keys map-vals
  get assoc]

 [rewrite-clj.impl.zip.subedit
  edit-node
  subedit-node
  subzip]

 [rewrite-clj.impl.zip.walk
  prewalk
  postwalk]

 [rewrite-clj.impl.zip.whitespace
  whitespace? linebreak?
  whitespace-or-comment?
  skip skip-whitespace
  skip-whitespace-left
  insert-space-left insert-space-right
  insert-newline-left insert-newline-right
  prepend-space append-space
  prepend-newline append-newline]

 {:sym-to-pattern "@@orig-sym@@*"
  :doc-to-pattern "Call zipper `@@orig-sym@@` function directly.\n\n@@orig-doc@@"}

 [rewrite-clj.impl.custom-zipper.core
  right left up down
  next prev
  rightmost leftmost
  replace edit remove
  insert-left insert-right])

;; ## DEPRECATED

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Use `string` instead."
  [zloc]
  (string zloc))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Use `root-string` instead."
  [zloc]
  (root-string zloc))
