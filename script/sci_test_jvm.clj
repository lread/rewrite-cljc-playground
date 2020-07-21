#!/usr/bin/env bb

(ns sci_test_jvm
  (:require [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.shell :as shell])

(status/line :info "Exposing rewrite-cljc API to sci")
(shell/command ["clojure" "-A:script" "-m" "sci-test-gen-publics"])

(status/line :info "Interpreting tests with sci from using JVM")
(shell/command ["clojure" "-A:sci-test" "-m" "sci-test.main" "--file" "script/sci_test_runner.clj" "--classpath" "test"])
