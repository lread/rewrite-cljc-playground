(ns rewrite-clj.zip
  "API for zipper navigation and updating Clojure/ClojureScript source code."
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.internal.zip.base]
            [rewrite-clj.internal.zip.move]
            [rewrite-clj.internal.zip.find]
            [rewrite-clj.internal.zip.edit]
            [rewrite-clj.internal.zip.insert]
            [rewrite-clj.internal.zip.remove]
            [rewrite-clj.internal.zip.seq]
            [rewrite-clj.internal.zip.subedit :include-macros true]
            [rewrite-clj.internal.zip.walk]
            [rewrite-clj.internal.zip.whitespace]
            [rewrite-clj.internal.custom-zipper.core :as z])
  (:require-macros rewrite-clj.zip
                   [rewrite-clj.internal.potemkin-cljs :refer [import-vars]]))

(import-vars
 [rewrite-clj.internal.custom-zipper.core
  node position root]

 [rewrite-clj.internal.zip.base
  child-sexprs
  edn* edn
  tag sexpr
  length
  value
  ;; TODO: not applicable for cljs: of-file
  of-string
  string root-string
  print print-root
  ]

 [rewrite-clj.internal.zip.edit
  replace edit splice
  prefix suffix]

 [rewrite-clj.internal.zip.find
  find find-next
  find-depth-first
  find-next-depth-first
  find-tag find-next-tag
  find-value find-next-value
  find-token find-next-token
  ;; clsj extras
  find-last-by-pos
  find-tag-by-pos]

 [rewrite-clj.internal.zip.insert
  insert-right insert-left
  insert-child append-child]

 [rewrite-clj.internal.zip.move
  left right up down prev next
  leftmost rightmost
  leftmost? rightmost? end?]

 [rewrite-clj.internal.zip.remove
  remove
  ;; cljs extras
  remove-preserve-newline]

 [rewrite-clj.internal.zip.seq
  seq? list? vector? set? map?
  map map-keys map-vals
  get assoc]

 [rewrite-clj.internal.zip.subedit
  edit-node
  subedit-node
  subzip]

 [rewrite-clj.internal.zip.walk
  prewalk
  postwalk]

 [rewrite-clj.internal.zip.whitespace
  whitespace? linebreak?
  whitespace-or-comment?
  skip skip-whitespace
  skip-whitespace-left
  insert-space-left insert-space-right
  insert-newline-left insert-newline-right
  prepend-space append-space
  prepend-newline append-newline])

;; ## Base Operations
;; TODO: figure out how to hook these up with defbase instead of def
(def right* rewrite-clj.internal.custom-zipper.core/right)
(def left* rewrite-clj.internal.custom-zipper.core/left)
(def up* rewrite-clj.internal.custom-zipper.core/up)
(def down* rewrite-clj.internal.custom-zipper.core/down)
(def next* rewrite-clj.internal.custom-zipper.core/next)
(def prev* rewrite-clj.internal.custom-zipper.core/prev)
(def rightmost* rewrite-clj.internal.custom-zipper.core/rightmost)
(def leftmost* rewrite-clj.internal.custom-zipper.core/leftmost)
(def replace* rewrite-clj.internal.custom-zipper.core/replace)
(def edit* rewrite-clj.internal.custom-zipper.core/edit)
(def remove* rewrite-clj.internal.custom-zipper.core/remove)
(def insert-left* rewrite-clj.internal.custom-zipper.core/insert-left)
(def insert-right* rewrite-clj.internal.custom-zipper.core/insert-right)

;; ## DEPRECATED

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Use `string` instead."
  [zloc]
  (string zloc))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Use `root-string` instead."
  [zloc]
  (root-string zloc))
