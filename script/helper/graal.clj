(ns helper.graal
  (:require [helper.fs :as fs]
            [helper.status :as status]
            [helper.shell :as shell]
            [helper.env :as env]
            [helper.jdk :as jdk]
            [clojure.string :as string]
            [clojure.java.io :as io]))

(defn find-prog [prog-name]
  (or (fs/on-path prog-name)
      (fs/at-path (str (io/file (System/getenv "JAVA_HOME") "bin")) prog-name)
      (fs/at-path (str (io/file (System/getenv "GRAALVM_HOME") "bin")) prog-name)))

(defn find-graal-native-image []
  (status/line :info "Locate GraalVM native-image")
  (let [res
        (or (find-prog (if (= :win (env/get-os)) "native-image.cmd" "native-image"))
            (if-let [gu (find-prog (if (= :win (env/get-os)) "gu.cmd" "gu"))]
              (do
                (status/line :detail "GraalVM native-image not found, attempting install")
                (shell/command [gu "install" "native-image"])
                (or (find-prog "native-image")
                    (status/fatal "failed to install GraalVM native-image, check your GraalVM installation" 1)))
              (status/fatal "GraalVM native image not found nor its installer, check your GraalVM installation" 1)))]
    (status/line :detail res)
    res))

(defn clean []
  (status/line :info "Clean")
  (fs/delete-file-recursively ".cpcache" true)
  (fs/delete-file-recursively "classes" true)

  (.mkdirs (io/file "classes"))
  (.mkdirs (io/file "target"))
  (status/line :detail "all clean"))

(defn aot-compile-sources [classpath ns]
  (status/line :info "AOT compile sources")
  (shell/command ["java"
                  "-Dclojure.compiler.direct-linking=true"
                  "-cp" classpath
                  "clojure.main"
                  "-e" (str "(compile '" ns ")")]))

(defn compute-classpath [alias jdk11-alias]
  (status/line :info "Compute classpath")
  (let [jdk-major-version (jdk/get-jdk-major-version)
        reflection-fix? (>= jdk-major-version 11)]
    (status/line :detail (str "JDK major version seems to be " jdk-major-version "; "
                              (if reflection-fix? "including" "excluding") " reflection fixes." ))
    (let [alias-opt (str "-A" alias (when reflection-fix? (str ":" jdk11-alias)))
          classpath (-> (shell/command ["clojure" alias-opt "-Spath"] {:out-to-string? true})
                        :out
                        string/trim)]
      (println "\nClasspath:")
      (println (str "- " (string/join "\n- " (fs/split-path-list classpath))))
      classpath)))

(defn run-native-image [{:keys [:graal-native-image :graal-reflection-fname
                                :target-exe :classpath :native-image-xmx
                                :entry-class]}]
  (status/line :info "Graal native-image compile AOT")
  (fs/delete-file-recursively target-exe true)
  (let [native-image-cmd (->> [graal-native-image
                               (str "-H:Name=" target-exe)
                               "-H:+ReportExceptionStackTraces"
                               "-J-Dclojure.spec.skip-macros=true"
                               "-J-Dclojure.compiler.direct-linking=true"
                               (when graal-reflection-fname
                                 (str "-H:ReflectionConfigurationFiles=" graal-reflection-fname))
                               "--initialize-at-run-time=java.lang.Math$RandomNumberGeneratorHolder"
                               "--initialize-at-build-time"
                               "-H:Log=registerResource:"
                               "--enable-all-security-services"
                               "--verbose"
                               "--no-fallback"
                               "--no-server"
                               "--report-unsupported-elements-at-runtime"
                               "-cp" (str classpath java.io.File/pathSeparator "classes")
                               (str "-J-Xmx" native-image-xmx)
                               entry-class]
                              (remove nil?))
         time-cmd (let [os (env/get-os)]
                    (case os
                        :mac ["command" "time" "-l"]
                        :unix ["command" "time" "-v"]
                        (status/line :warn (str "I don't know how to get run stats (user/real/sys CPU, RAM use, etc) for a command on " os))))]

    (shell/command (concat time-cmd native-image-cmd))))
