#!/usr/bin/env bb

(ns gen-api-diffs
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.fs :as fs]
         '[helper.shell :as shell])

(defn install-locally []
  ;; TODO: maybe this should change after dev is over?
  ;; TODO: but for now working on local copy of rewrite-cljc
  (status/line :info "installing rewrite-cljc locally")
  (shell/command ["mvn" "install"]))

(defn wipe-rewrite-cljc-diff-cache [ rewrite-cljc-maven-coords]
  (let [cache-dir (io/file "./.diff-apis/.cache")
        proj-cache-prefix (string/replace rewrite-cljc-maven-coords "/" "-")]
    (when-let [proj-cache-dir (and (.exists cache-dir)
                                 (.isDirectory cache-dir)
                                 (first (filter
                                         #(string/starts-with? (str (.getName %)) proj-cache-prefix)
                                         (.listFiles cache-dir))))]
      (fs/delete-file-recursively proj-cache-dir true))))

(defn clean [{:keys [:report-dir]} rewrite-cljc-coords]
  (status/line :info "Clean")
  (status/line :detail "- report dir")
  (fs/delete-file-recursively report-dir true)
  (.mkdirs (io/file report-dir))
  (status/line :detail "- cached metdata for rewrite-cljc (because that's the thing that is changing, right?)")
  (wipe-rewrite-cljc-diff-cache rewrite-cljc-coords)
  (status/line :detail "all done"))

(defn describe-proj [project]
  (str (or (:as-coords project)
           (:coords project)) " " (:version project) " " (:lang project)))

(defn diff-apis [{:keys [:notes-dir :report-dir]} projecta projectb report-name extra-args]
  (status/line :info (str "Diffing " (describe-proj projecta) " and " (describe-proj projectb)))
  (shell/command (concat ["clojure" "-A:diff-apis"]
                         (map projecta [:coords :version :lang])
                         (map projectb [:coords :version :lang])
                         ["--arglists-by" ":arity-only"
                          "--notes" (str (io/file notes-dir (str report-name ".adoc")))
                          "--report-format" ":asciidoc"
                          "--report-filename" (str  (io/file report-dir (str report-name ".adoc")))]
                         extra-args)))

(defn main []
  (let [opts {:notes-dir "doc/diff-notes"
              :report-dir "doc/generated/api-diffs"}
        rewrite-clj      {:coords "rewrite-clj" :version "0.6.1" :lang "clj"}
        rewrite-cljs     {:coords "rewrite-cljs" :version "0.4.5" :lang "cljs"}
        ;; TODO: rewrite-cljc coords will become real on first release
        rewrite-cljc-clj {:coords "lread/rewrite-cljc-playground" :as-coords "rewrite-cljc" :version "1.0.0-alpha" :lang "clj"}
        rewrite-cljc-cljs (assoc rewrite-cljc-clj :lang "cljs")
        existing-to-cljc-args ["--exclude-namespace" "rewrite-cljc"
                               "--exclude-namespace" "rewrite-clj.potemkin"
                               "--exclude-namespace" "rewrite-cljc.potemkin"
                               "--exclude-namespace" "rewrite-cljc.potemkin.cljs"
                               "--exclude-namespace" "rewrite-cljc.potemkin.clojure"
                               "--exclude-namespace" "rewrite-cljc.potemkin.helper"
                               "--exclude-namespace" "rewrite-cljc.custom-zipper.switchable"
                               "--exclude-namespace" "rewrite-cljc.interop"
                               "--replace-b-namespace" "^rewrite-cljc/rewrite-clj"]
        to-self-args ["--exclude-namespace" "rewrite-cljc.potemkin.clojure"]
        documented-only-args ["--exclude-with" ":no-doc" "--exclude-with" ":skip-wiki"]]
    (install-locally)
    (clean opts (:coords rewrite-cljc-clj))
    (diff-apis opts rewrite-clj       rewrite-cljs      "rewrite-clj-and-rewrite-cljs"       [])
    (diff-apis opts rewrite-clj       rewrite-cljc-clj  "rewrite-clj-and-rewrite-cljc-clj"   existing-to-cljc-args)
    (diff-apis opts rewrite-cljs      rewrite-cljc-cljs "rewrite-cljs-and-rewrite-cljc-cljs" existing-to-cljc-args)
    (diff-apis opts rewrite-cljc-cljs rewrite-cljc-clj  "rewrite-cljc"                       to-self-args)
    (diff-apis opts rewrite-cljc-cljs rewrite-cljc-clj  "rewrite-cljc-documented-only"       (concat to-self-args documented-only-args))))

(main)
