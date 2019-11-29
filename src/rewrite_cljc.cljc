(ns rewrite-cljc
  "APIs to navigate and update Clojure/ClojureScript/EDN source code.

  Start with the [[rewrite-cljc.parser]] or [[rewrite-cljc.zip]] to ingest your source code,
  then use the [[rewrite-cljc.zip]] to navigate and/or change it. [[rewrite-cljc.node]]
  will help you to work with nodes in the zipper tree.

  [[rewrite-cljc.paredit]] first appeared in the ClojureScript only version of
  rewrite-clj and supports structured editing of the zipper tree.")
