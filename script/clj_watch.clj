#!/usr/bin/env bb

(ns clj-watch
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.shell :as shell]
         '[helper.status :as status])

(status/line :info "launching kaocha watch on clojure sources")
(shell/command (concat ["clojure" "-A:test-common:kaocha" "--watch" ] *command-line-args*))
