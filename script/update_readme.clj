(ns update-readme
  (:require [clojure.string :as string]
            [clojure.edn :as edn]))

(def contributions-lookup
  {:code-rewrite-cljc "💻 rewrite-cljc"
   :code-rewrite-cljs "💻 https://github.com/clj-commons/rewrite-cljs[rewrite-cljs]"
   :code-rewrite-clj "💻 https://github.com/xsc/rewrite-clj[rewrite-clj]"
   :encouragement "🌞 encouragement"
   :education "🎓 education"})


(defn- generate-asciidoc-rows [contributors]
  (for [{:keys [github-id contributions]} contributors]
    (str "|image:https://github.com/" github-id ".png?size=110[role=\"thumb left related\",width=110]\n"
         "https://github.com/" github-id "[@" github-id "] +\n"
         (apply str (map #(if-let [c (% contributions-lookup)]
                            (str  c " +\n")
                            (println "WARN: specified contribution key for" github-id "does not exist" %))
                         contributions)))))

(defn- generate-asciidoc-table [contributors]
  (str "[cols=\"{contrib-cols}*<.<\",stripes=none,grid=none,frame=none]\n"
       "|====\n"
       "\n"
       (apply str (generate-asciidoc-rows contributors))
       "|===="))

(defn- update-readme-text [old-text new-table]
  (string/replace old-text
                  #"(?s)// AUTO-GENERATED:CONTRIBUTORS-START.*// AUTO-GENERATED:CONTRIBUTORS-END"
                  (str "// AUTO-GENERATED:CONTRIBUTORS-START\n" new-table "\n// AUTO-GENERATED:CONTRIBUTORS-END")))

(defn update-readme-file [contributors-filename readme-filename]
  (let [old-text (slurp readme-filename)
        contributors (edn/read-string (slurp contributors-filename))
        new-text (update-readme-text old-text (generate-asciidoc-table contributors))]
    (if (not (= old-text new-text))
      (spit readme-filename new-text)
      (println "INFO" readme-filename "unchanged"))))

(defn -main []
  (let [readme-filename "README.adoc"
        contributors-filename "doc/contributors.edn"]
    (println "INFO updating contributors" readme-filename "from" contributors-filename)
    (update-readme-file contributors-filename readme-filename)))
