#!/usr/bin/env bb

(ns clj_tests
  (:require [babashka.classpath :as cp]
            [clojure.string :as string]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.shell :as shell])

(def allowed-versions '("1.9" "1.10"))
(def default-version "1.10")

(defn usage-error [ ]
  (status/line :error "usage")
  (println (string/join "\n" ["Usage: clj_tests.clj <clojure version>"
                              ""
                              "Where <clojure version> is one of: "
                              (str " "(string/join "\n " allowed-versions))
                              (str "Defaults to " default-version)]))
  (System/exit 1))

(defn run-tests[clojure-version]
  (status/line :info (str "testing clojure source against clojure v" clojure-version))
  (shell/command ["clojure"
                  (str "-A:test-common:kaocha:" clojure-version)
                  "--reporter" "documentation"
                  "--plugin" "kaocha.plugin/junit-xml"
                  "--junit-xml-file"  (str "target/out/test-results/clj-v" clojure-version "/results.xml")]))

(defn main [args]
  (let [clojure-version (or (first args) default-version)]
    (if (some #{clojure-version} allowed-versions)
      (run-tests clojure-version)
      (usage-error))))

(main *command-line-args*)
