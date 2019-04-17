(ns rewrite-clj
  "APIs to navigate and update Clojure/ClojureScript source code.

  Start with the [[rewrite-clj.parser]] API to injest your source code, then use the
  [[rewrite-clj.zip]] API to navigate and/or change it. The [[rewrite-clj.node]] API
  will help you to work with nodes in the zipper tree.

  The [[rewrite-clj.paredit]] api first appeared in the ClojureScript only version of
  rewrite-clj and supports structured editing.")
