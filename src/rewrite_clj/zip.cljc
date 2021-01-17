(ns rewrite-clj.zip
  "A rich API for navigating and updating Clojure/ClojureScripti/EDN source code via a zipper tree.

  The zipper holds a tree of nodes representing source code. It also holds your current location while navigating
  through the tree and any pending changes you have made. Changes are applied back into the tree
  when invoking root functions.

  Although they are preserved, whitespace and comment nodes are normally skipped when navigating through the tree.
  There are times when you will want to include whitespace and comment nodes, and as you see below, provisions are
  available to do so.

  It is good to remember that while some function names convey mutation, we are never changing anything, we are
  instead returning modified copies.

  Some conventions in the code and docstrings:
  - `zloc` is the used as the argument name for our zipper
  - \"current node in `zloc`\" is shorthand for: node at current location in zipper `zloc`

  Because this API contains many functions, we offer the following categorized listing:

  **Create a zipper**
  [[edn]]
  [[edn*]]
  [[of-string]]
  [[of-file]]

  **Move**
  [[left]]
  [[right]]
  [[up]]
  [[down]]
  [[prev]]
  [[next]]
  [[leftmost]]
  [[rightmost]]

  **Move without skipping whitespace and comments**
  [[left*]]
  [[right*]]
  [[up*]]
  [[down*]]
  [[prev*]]
  [[next*]]
  [[leftmost*]]
  [[rightmost*]]

  **Whitespace/comment aware skip**
  [[skip]]
  [[skip-whitespace]]
  [[skip-whitespace-left]]

  **Test for whitespace**
  [[whitespace?]]
  [[linebreak?]]
  [[whitespace-or-comment?]]

  **Test location**
  [[leftmost?]]
  [[rightmost?]]
  [[end?]]

  **Test data type**
  [[seq?]]
  [[list?]]
  [[vector?]]
  [[set?]]
  [[map?]]
  [[namespaced-map?]]

  **Find**
  [[find]]
  [[find-next]]
  [[find-depth-first]]
  [[find-next-depth-first]]
  [[find-tag]]
  [[find-next-tag]]
  [[find-value]]
  [[find-next-value]]
  [[find-token]]
  [[find-next-token]]
  [[find-last-by-pos]]
  [[find-tag-by-pos]]

  **Inspect**
  [[node]]
  [[position]]
  [[position-span]]
  [[tag]]
  [[length]]

  **Convert**
  [[sexpr]]
  [[child-sexprs]]
  [[reapply-context]]

  **Update**
  [[replace]]
  [[edit]]
  [[splice]]
  [[prefix]]
  [[suffix]]
  [[insert-right]]
  [[insert-left]]
  [[insert-child]]
  [[insert-space-left]]
  [[insert-space-right]]
  [[insert-newline-left]]
  [[insert-newline-right]]
  [[append-child]]
  [[remove]]
  [[remove-preserve-newline]]
  [[root]]

  **Update without whitespace treatment**
  [[replace*]]
  [[edit*]]
  [[insert-left*]]
  [[insert-right*]]
  [[insert-child*]]
  [[append-child*]]
  [[remove*]]

  **Isolated update without changing location**
  [[edit-node]]
  [[subedit-node]]
  [[subzip]]
  [[prewalk]]
  [[postwalk]]
  [[edit->]]
  [[edit->>]]
  [[subedit->]]
  [[subedit->>]]

  **Sequence operations**
  [[map]]
  [[map-keys]]
  [[map-vals]]
  [[get]]
  [[assoc]]

  **Stringify**
  [[string]]
  [[root-string]]

  **Output**
  [[print]]
  [[print-root]]"
  (:refer-clojure :exclude [next find replace remove
                            seq? map? vector? list? set?
                            print map get assoc])
  (:require [rewrite-clj.custom-zipper.core]
            #?(:clj [rewrite-clj.potemkin.clojure :refer [import-vars import-vars-with-mods]])
            [rewrite-clj.zip.base]
            [rewrite-clj.zip.context]
            [rewrite-clj.zip.editz]
            [rewrite-clj.zip.findz]
            [rewrite-clj.zip.insert]
            [rewrite-clj.zip.move]
            [rewrite-clj.zip.removez]
            [rewrite-clj.zip.seqz]
            [rewrite-clj.zip.subedit #?@(:cljs [:include-macros true])]
            [rewrite-clj.zip.walk]
            [rewrite-clj.zip.whitespace])
  #?(:cljs (:require-macros [rewrite-clj.potemkin.cljs :refer [import-vars import-vars-with-mods]]
                            [rewrite-clj.zip])))

#?(:clj (set! *warn-on-reflection* true))

(import-vars
 [rewrite-clj.custom-zipper.core
  node position position-span root]

 [rewrite-clj.zip.base
  child-sexprs
  edn* edn
  tag sexpr
  length
  value
  #?(:clj of-file)
  of-string
  string root-string
  print print-root]

 [rewrite-clj.zip.editz
  replace edit splice
  prefix suffix]

 [rewrite-clj.zip.context
  reapply-context]

 [rewrite-clj.zip.findz
  find find-next
  find-depth-first
  find-next-depth-first
  find-tag find-next-tag
  find-value find-next-value
  find-token find-next-token
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
  remove-preserve-newline]

 [rewrite-clj.zip.seqz
  seq? list? vector? set? map? namespaced-map?
  map map-keys map-vals
  get assoc]

 [rewrite-clj.zip.subedit
  edit-node
  subedit-node
  subzip
  edit-> edit->>
  subedit-> subedit->>]

 [rewrite-clj.zip.walk
  prewalk
  postwalk]

 [rewrite-clj.zip.whitespace
  whitespace? linebreak?
  whitespace-or-comment?
  skip skip-whitespace
  skip-whitespace-left
  insert-space-left insert-space-right
  insert-newline-left insert-newline-right
  prepend-space append-space
  prepend-newline append-newline])

(import-vars-with-mods
 {:sym-to-pattern "@@orig-name@@*"
  :doc-to-pattern "Raw version of [[@@orig-name@@]].\n\n@@orig-doc@@\n\nNOTE: This function does not skip, nor provide any special handling for whitespace/comment nodes."}
 [rewrite-clj.custom-zipper.core
  right left up down
  next prev
  rightmost leftmost
  replace edit remove
  insert-left insert-right
  insert-child
  append-child])

;; ## DEPRECATED

(defn ^{:deprecated "0.4.0"} ->string
  "DEPRECATED. Renamed to [[string]]."
  [zloc]
  (rewrite-clj.zip.base/string zloc))

(defn ^{:deprecated "0.4.0"} ->root-string
  "DEPRECATED. Renamed to [[root-string]]."
  [zloc]
  (rewrite-clj.zip.base/root-string zloc))
