(ns helper.graal
  (:require [helper.fs :as fs]
            [helper.status :as status]
            [helper.shell :as shell]
            [clojure.java.io :as io]))

(defn find-prog [prog-name]
  (or (fs/on-path prog-name)
      (fs/at-path (str (io/file (System/getenv "JAVA_HOME") "bin")) prog-name)
      (fs/at-path (str (io/file (System/getenv "GRAALVM_HOME") "bin")) prog-name)))

(defn find-graal-native-image []
  (status/line :info "Locate GraalVM native-image")
  (let [res
        (or (find-prog "native-image")
            (if-let [gu (find-prog "gu")]
              (do
                (status/line :detail "GraalVM native-image not found, attempting install")
                (shell/command [gu "install" "native-image"])
                (or (find-prog "native-image")
                    (status/fatal "failed to install GraalVM native-image, check your GraalVM installation" 1)))
              (status/fatal "GraalVM native image not found nor its installer, check your GraalVM installation" 1)))]
    (status/line :detail res)
    res))
