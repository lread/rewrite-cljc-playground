#!/usr/bin/env bb

(ns get_version
  "Prints calculated version"
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[version])

(defn main []
  (println (version/calculate)))

(main)
