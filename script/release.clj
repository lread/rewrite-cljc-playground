#!/usr/bin/env bb
;; TODO: needs some testing
(ns release
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.shell :as shell])

(defn usage[]
  (->> ["Usage release <action>"
        ""
        "Where action is one of:"
        " version - calculate and display version"
        " prep    - prepare for publication by updating pom.xml with current deps and"
        "           version info"
        " local   - prep and install to local maven repository for local testing"
        " publish - does a prep then publishes to clojars"]
       (string/join "\n")))

(defn validate-args [args]
  (let [action (first args)]
    (if (and (= 1 (count args))
             (some #{action} '("version" "prep" "local" "publish")))
      {:action action}
      {:exit-message (usage) :exit-code 1})))

(defn exit [code msg]
  (if (zero? code)
    (status/line :detail msg)
    (status/line :error msg))
  (System/exit code))


(defn calculate-version []
  (let [{:keys [:major :minor :qualifier]} (edn/read-string (slurp "./script/resources/version.edn"))
        tags (-> (shell/command ["git" "--no-pager" "tag" "--sort=creatordate"]
                                {:out-to-string? true})
                 :out
                 (string/split-lines))
        version-pattern (re-pattern (str "v" major "\\." minor "\\..*"))
        earliest-tag (->> tags
                          (filter #(re-matches version-pattern %))
                          first)
        patch (if (not earliest-tag)
                "0"
                (-> (shell/command ["git" "rev-list" (str earliest-tag "..") "--count"]
                                   {:out-to-string? true})
                    :out
                    (string/trim)))]
    (str major "." minor "." patch (when qualifier (str "-" qualifier)))))

(defn prepare-for-publish []
  (status/line :info "Prepare for publish")
  (let [tag (-> (shell/command ["git" "rev-parse" "HEAD"]
                               {:out-to-string? true})
                :out
                string/trim)
        version (calculate-version)]
    (status/line :info "1 of 3) reflecting deps.edn to pom.xml")
    (shell/command ["clojure" "-Spom"])
    (status/line :info (str "2 of 3) setting pom.xml tag to " tag))
    (shell/command ["mvn" "versions:set-scm-tag" (str "-DnewTag=" tag) "-DgenerateBackupPoms=false"])
    (status/line :info (str "3 of 3) setting pom.xml version to " version))
    (shell/command ["mvn" "versions:set" (str "-DnewVersion=" version) "-DgenerateBackupPoms=false"])))

(defn repo-clean? []
  (string/blank? (shell/command ["git" "status" "--procelain"] {:out-to-string? true})))

(defn get-from-pom [ pom-exression ]
  (-> (shell/command ["mvn" "help:evaluate" (str "-Dexpression=" pom-exression) "-q" "-DforceStdout"]
                     {:out-to-string? true})
      :out
      string/trim))

(defn local-install []
  (prepare-for-publish)
  (status/line :info "Installing to local maven repository")
  (shell/command ["mvn" "install"]))

(defn publish []
  (status/line :info "Publishing to clojars")
  (when (not (repo-clean?))
    (status/fatal "Repo is not clean." 1))
  (prepare-for-publish)
  (let [version (calculate-version)
        tag (str "v" version)]
    (status/line :info (str "1 of 4) tagging git repo with: " tag))
    (shell/command ["git" "tag" tag])
    (shell/command ["git" "push" "--tags"])

    (status/line :info "2 of 4) deploying to clojars")
    (shell/command ["mvn" "deploy"])

    (status/line :info "3 of 4) push updated pom.xml")
    (shell/command ["git" "add" "pom.xml"])
    (shell/command ["git" "commit" "-m" (str "Update versions in pom.xml to " version)])
    (shell/command ["git" "push"])

    ;; TODO: low priority, is curl available on Windows?
    (status/line :info "4 of 4) trigger build on cljdoc")
    (let [group-id (get-from-pom "project.groupId")
          artifact-id (get-from-pom "project.artifactId")]
      (shell/command ["curl" "-X" "POST"
                      "-d" (str "project=" group-id "/" artifact-id)
                      "-d" (str "version=" version)
                      "https://cljdoc.org/api/request-build2"]))
    (status/line :info "all done")))

(defn main [args]
  (let [{:keys [:action :exit-message :exit-code]} (validate-args args)]
    (if exit-message
      (exit exit-code exit-message)
      (case action
        "version" (println (calculate-version))
        "prep"    (prepare-for-publish)
        "local"   (local-install)
        "publish" (publish)))))

(main *command-line-args*)

(comment
  (calculate-version)
  (some #{"version"} '("version" "prep" "publish"))
  (validate-args ["version"]))
