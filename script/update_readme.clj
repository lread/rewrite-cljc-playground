(ns update-readme
  "Script to update README.adoc to credit contributors
  Run manually as needed.
  This is a bit of an experiment using clojure instead of bash."
  (:require [clojure.string :as string]
            [clojure.edn :as edn]
            [hiccup.page :refer [html5 include-css]]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io])
  (:import (java.nio.file Files Paths CopyOption StandardCopyOption)
           (java.nio.file.attribute FileAttribute)
           (org.apache.commons.io FileUtils)))


(def contributions-lookup
  {:code-rewrite-cljc "💻 rewrite-cljc"
   :code-rewrite-cljs "💻 rewrite-cljs"
   :code-rewrite-clj "💻 rewrite-clj"
   :encouragement "🌞 encouragement"
   :education     "🎓 enlightenment"
   :original-author "👑 original author"})


(defn- generate-asciidoc [contributors {:keys [images-dir image-width]}]
  (str ":imagesdir: " images-dir "\n"
       "[.float-group]\n"
       "--\n"
       (apply str (for [{:keys [github-id]} contributors]
                    (str "image:" github-id ".png[" github-id ",role=\"left\",width=" image-width ",link=\"https://github.com/" github-id "\"]\n")))
       "--\n"))

(defn- update-readme-text [old-text marker-id new-content]
  (let [marker (str "// AUTO-GENERATED:" marker-id )
        marker-start (str marker "-START")
        marker-end (str marker "-END")]
    (string/replace old-text
                    (re-pattern (str "(?s)" marker-start ".*" marker-end))
                    (str marker-start "\n" (string/trim new-content) "\n" marker-end))))


(defn update-readme-file [contributors readme-filename image-info]
  (println "--[updating" readme-filename "]--")
  (let [old-text (slurp readme-filename)
        new-text (-> old-text
                     (update-readme-text "CONTRIBUTORS" (generate-asciidoc (:contributors contributors) image-info))
                     (update-readme-text "FOUNDERS" (generate-asciidoc (:founders contributors) image-info))
                     (update-readme-text "MAINTAINERS" (generate-asciidoc (:maintainers contributors) image-info)))]
    (if (not (= old-text new-text))
      (do
        (spit readme-filename new-text)
        (println readme-filename "text updated") )
      (println readme-filename "text unchanged"))))

(defn generate-contributor-html [ github-id contributions]
  (html5
   [:head
    (include-css "https://fonts.googleapis.com/css?family=Fira+Code&display=swap")
    [:style
        "* {
          -webkit-font-smoothing: antialiased;
          -moz-osx-font-smoothing: grayscale;}
         body {
           font-family: 'Fira Code', monospace;
           margin: 0;}
         .card {
           min-width: 295px;
           float: left;
           border-radius: 5px;
           border: 1px solid #CCCCCC;
           padding: 4px;
           margin: 0 5px 5px 0;
           box-shadow: 4px 4px 3px grey;
           background-color: #F4F4F4;}
         .avatar {
           float: left;
           height: 110px;
           border-radius: 4px;
           padding: 0;
           margin-right: 6px; }
         .image { margin: 2px;}
         .text {
           margin-left: 2px;
           padding: 0}
         .contrib { margin: 0; }
         .name {
           font-size: 1.20em;
           margin: 0 3px 5px 0;}"]]
   [:div.card
    [:img.avatar {:src (str "https://github.com/" github-id ".png?size=110")}]
    [:div.text
     [:p.name (str "@" github-id)]
     [:div.contribs
      (doall
       (for [c contributions]
         (when-let [c-text (c contributions-lookup)]
           [:p.contrib c-text])))]]]))

(defn- str->Path [spath]
  (Paths/get spath (into-array String [])))

  (defn- temp-Path [prefix]
  (Files/createTempDirectory prefix (into-array FileAttribute [])))

(defn- move-Path [source target]
  (FileUtils/deleteDirectory (.toFile target))
  (.mkdirs (.toFile target))
  (Files/move source target (into-array CopyOption
                                        [(StandardCopyOption/ATOMIC_MOVE)
                                         (StandardCopyOption/REPLACE_EXISTING)])))

(defn- chrome []
  (let [mac-chrome "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        linux-chrome "chrome"]
    (cond
      (.canExecute (io/file mac-chrome)) mac-chrome
      :else linux-chrome)))

(defn- chrome-info []
  (try
    (let [chrome (chrome)
          result (shell/sh chrome "--version")]
      (when (= 0 (:exit result))
        {:exe chrome
         :version (string/trim (:out result))}))
    (catch Exception _e)))

(defn- generate-image [target-dir github-id contributions image-opts]
  (let [html-file (str target-dir "/temp.html")]
    (try
      (spit html-file (generate-contributor-html github-id contributions))
      (let [result (shell/sh (chrome)
                             "--headless"
                             (str "--screenshot=" target-dir "/" github-id ".png")
                             (str "--window-size=" (:image-width image-opts) ",125")
                             "--default-background-color=0"
                             "--hide-scrollbars"
                             html-file)]
        (when (not (= 0 (:exit result)))
          (throw (ex-info "png generation failed" result))))
      (finally
        (FileUtils/deleteQuietly (io/file html-file))))))

(defn- generate-contributor-images [contributors image-opts]
  (println "--[generating images]--")
  (let [work-dir (temp-Path "rewrite-cljc-update-readme")]
    (try
      (doall
       (for [ctype (keys contributors)]
         (do
           (println ctype)
           (doall
            (for [{:keys [github-id contributions]} (ctype contributors)]
              (do
                (println " " github-id)
                (generate-image (str work-dir) github-id contributions image-opts)))))))
      (let [target-path (str->Path (:images-dir image-opts))]
        (move-Path work-dir target-path))
      (catch java.lang.Exception e
        (FileUtils/forceDeleteOnExit (.toFile work-dir))
        (throw e)))))

(defn- check-prerequesites []
  (println "--[checking prerequesites]--")
  (let [chrome-info (chrome-info)]
    (if chrome-info
      (println (str "found chrome:" (:exe chrome-info) "\n"
                    "version:" (:version chrome-info)))
      (println "* error: did not find google chrome - need it to generate images."))
    chrome-info))

(defn -main []
  (let [readme-filename "README.adoc"
        contributors-source "doc/contributors.edn"
        image-opts {:image-width 310
                    :images-dir "./doc/generated/contributors"}
        contributors (edn/read-string (slurp contributors-source))]
    (println "--[updating docs to honor those who contributed]--")
    (when (not (check-prerequesites))
      (System/exit 1))
    (println "contributors source:" contributors-source)
    (generate-contributor-images contributors image-opts)
    (update-readme-file contributors readme-filename image-opts)
    (println "SUCCESS"))
  (shutdown-agents))
