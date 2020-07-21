(ns version
  "Calculates version as number of commits since first major.minor tag
   If there is no major.minor yet, assume, but do not create, major.minor.0"
  (:require [clojure.string :as string]
            [babashka.classpath :as cp]))

(cp/add-classpath "./script")
(require '[helper.shell :as shell])

;; Source of truth for major.minor - change them here manually when it makes sense
(def version-major "1")
(def version-minor "0")
(def qualifier "-alpha")

(defn calculate []
  (let [tags (-> (shell/command ["git" "--no-pager" "tag" "--sort=creatordate"]
                                {:out-to-string? true})
                 :out
                 (string/split-lines))
        version-pattern (re-pattern (str "v" version-major "\\." version-minor "\\..*"))
        earliest-tag (->> tags
                          (filter #(re-matches version-pattern %))
                          first)
        version-patch (if (not earliest-tag)
                        "0"
                        (-> (shell/command ["git" "rev-list" (str earliest-tag "..") "--count"]
                                           {:out-to-string? true})
                            :out
                            (string/trim)))]
    (str version-major "." version-minor "." version-patch qualifier)))
