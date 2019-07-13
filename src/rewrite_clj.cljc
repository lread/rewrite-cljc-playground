(ns rewrite-clj
  "APIs to navigate and update Clojure/ClojureScript/EDN source code.

  Start with the [[rewrite-clj.parser]] or [[rewrite-clj.zip]] to ingest your source code,
  then use the [[rewrite-clj.zip]] to navigate and/or change it. [[rewrite-clj.node]]
  will help you to work with nodes in the zipper tree.

  [[rewrite-clj.paredit]] first appeared in the ClojureScript only version of
  rewrite-clj and supports structured editing of the zipper tree.")
