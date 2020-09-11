#!/usr/bin/env bb

(ns lint
  (:require [babashka.classpath :as cp]
            [clojure.java.io :as io]))

(cp/add-classpath "./script")
(require '[helper.shell :as shell]
         '[helper.status :as status])

(defn cache-exists? []
  (.exists (io/file ".clj-kondo/.cache")))

(defn lint[]
  (if (not (cache-exists?))
    (status/line :info "linting and building cache")
    (status/line :info "linting"))

  (let [lint-args (if (not (cache-exists?))
                    [(shell/command ["clojure" "-R:test-common:script" "-C:test-common:script" "-Spath"]
                                    {:out-to-string? true}) "--cache"]
                    ["src" "test" "script"])
       {:keys [:exit]} (shell/command-no-exit
                        (concat ["clojure" "-A:clj-kondo"
                                 "--lint"]
                                lint-args
                                ["--config" "{:output {:include-files [\"^src\" \"^test\" \"^script\"]}}"]))]
    (when (not (some #{exit} '(0 2 3)))
      (status/line :error (str "clj-kondo existed with unexpected exit code: " exit)))
    (System/exit exit)))

(lint)
