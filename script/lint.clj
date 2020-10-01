#!/usr/bin/env bb

(ns lint
  (:require [babashka.classpath :as cp]
            [clojure.java.io :as io]))

(cp/add-classpath "./script")
(require '[helper.env :as env]
         '[helper.shell :as shell]
         '[helper.status :as status])

(defn cache-exists? []
  (.exists (io/file ".clj-kondo/.cache")))

(defn lint[]
  (env/assert-min-clojure-version)
  (if (not (cache-exists?))
    (status/line :info "linting and building cache")
    (status/line :info "linting"))

  (let [lint-args (if (not (cache-exists?))
                    [(shell/command ["clojure" "-A:test-common:script" "-Spath"]
                                    {:out-to-string? true}) "--cache"]
                    ["src" "test" "script"])
       {:keys [:exit]} (shell/command-no-exit
                        (concat ["clojure" "-M:clj-kondo"
                                 "--lint"]
                                lint-args
                                ["--config" ".clj-kondo/ci-config.edn"]))]
    (when (not (some #{exit} '(0 2 3)))
      (status/line :error (str "clj-kondo existed with unexpected exit code: " exit)))
    (System/exit exit)))

(lint)
