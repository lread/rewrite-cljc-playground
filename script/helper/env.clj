(ns helper.env
  (:require [clojure.string :as string]
            [helper.shell :as shell]))

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp #(re-find %1 %2) os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))

(defn get-jdk-major-version
  "Returns jdk major version converting old style appropriately. (ex 1.8 returns 8)"
  []
  (let [version
        (->> (shell/command ["java" "-version"] {:err-to-string? true})
             :err
             (re-find #"version \"(\d+)\.(\d+)\.\d+.*\"")
             rest
             (map #(Integer/parseInt %)))]
    (if (= (first version) 1)
      (second version)
      (first version))))
