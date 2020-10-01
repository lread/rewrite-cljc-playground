(ns helper.env
  (:require [babashka.classpath :as cp]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(cp/add-classpath "./script")
(require '[helper.shell :as shell]
         '[helper.status :as status])

(def version-clj-dep '{:deps {version-clj/version-clj {:mvn/version "0.1.2"}}})
(cp/add-classpath (-> (shell/command ["clojure" "-Spath" "-Sdeps" (str version-clj-dep)] {:out-to-string? true})
                      :out
                      string/trim))

(require '[version-clj.core :as ver])

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp #(re-find %1 %2) os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))

(defn assert-min-clojure-version
  "Asserts minimum version of Clojure version"
  []
  (let [min-version "1.10.1.697"
        version
        (->> (shell/command ["clojure" "-Sdescribe"] {:out-to-string? true})
             :out
             edn/read-string
             :version)]
    (when (< (ver/version-compare version min-version) 0)
      (status/fatal (str "A minimum version of Clojure " min-version " required.\nFound version: " version)))))
