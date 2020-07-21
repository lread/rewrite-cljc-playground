#!/usr/bin/env bb

(ns sci_test_native
  (:require [clojure.java.io :as io]
            [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.env :as env]
         '[helper.shell :as shell])

(def exe-fname (if (= :win (env/get-os))
                 "target/sci-test-rewrite-cljc.exe"
                 "target/sci-test-rewrite-cljc"))

(when (not (.exists (io/file exe-fname)))
  (status/fatal (str "native image " exe-fname " not found, did you run script/sci_test_gen_native_image.clj yet?") 1))

(status/line :info "Interpreting tests with sci using natively compiled binary")
(shell/command [exe-fname "--file" "script/sci_test_runner.clj" "--classpath" "test"])
