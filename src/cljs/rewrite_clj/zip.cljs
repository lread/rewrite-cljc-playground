(ns rewrite-clj.zip
  "Client facing facade for zipper functions"
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.zip.base]
            [rewrite-clj.parser]
            [rewrite-clj.zip.move]
            [rewrite-clj.zip.findz]
            [rewrite-clj.zip.editz]
            [rewrite-clj.zip.insert]
            [rewrite-clj.zip.removez]
            [rewrite-clj.zip.seqz]
            [rewrite-clj.zip.subedit :include-macros true]
            [rewrite-clj.zip.walk]
            [rewrite-clj.custom-zipper.core :as z])
  (:require-macros [rewrite-clj.potemkin-cljs :refer [import-vars import-def]]))

(import-vars
 [rewrite-clj.custom-zipper.core
  node position root]

 [rewrite-clj.zip.base
  child-sexprs
  edn* edn
  tag sexpr
  length
  ;; TODO: value - deprecated in clj... wasn't in cljs
  ;; not applicable for cljs: of-file
   of-string
   string root-string
  ;; TODO: print print-root
  ]

 [rewrite-clj.zip.editz
  replace edit splice
  prefix suffix]

 [rewrite-clj.zip.findz
  find find-next
  find-depth-first
  find-next-depth-first
  find-tag find-next-tag
  find-value find-next-value
  find-token find-next-token
  ;; clsj extras
  find-last-by-pos
  find-tag-by-pos]

 [rewrite-clj.zip.insert
  insert-right insert-left
  insert-child append-child]

 [rewrite-clj.zip.move
  left right up down prev next
  leftmost rightmost
  leftmost? rightmost? end?]

 [rewrite-clj.zip.removez
  remove
  ;; cljs extras
  remove-preserve-newline]

 [rewrite-clj.zip.seqz
  seq? list? vector? set? map?
  map map-keys map-vals
  get assoc]

 [rewrite-clj.zip.subedit
  edit-node
  subedit-node]

 [rewrite-clj.zip.walk
  prewalk
  postwalk]

 [rewrite-clj.zip.whitespace
  whitespace? linebreak?
  whitespace-or-comment?
  skip skip-whitespace
  skip-whitespace-left
  prepend-space append-space
  prepend-newline append-newline])
