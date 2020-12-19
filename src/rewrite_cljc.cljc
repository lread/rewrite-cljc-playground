(ns rewrite-cljc
  "APIs to navigate and update Clojure/ClojureScript/EDN source code.

  Use [[rewrite-cljc.zip]] to ingest your source code into a zipper of nodes and then again to navigate and/or change it.

  Optionally use [[rewrite-cljc.parser]] to instead work with raw nodes.

  [[rewrite-cljc.node]] will help you to inspect and create nodes.

  [[rewrite-cljc.paredit]] first appeared in the ClojureScript only version of rewrite-clj and supports structured editing of the zipper tree.")
