#!/usr/bin/env bb

(ns sci-test-gen-native-image
  (:require
   [babashka.classpath :as cp]
   [clojure.java.io :as io]
   [clojure.string :as string]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.env :as env]
         '[helper.fs :as fs]
         '[helper.shell :as shell]
         '[helper.graal :as graal])

(def os (env/get-os))

(defn command
  ([args] (command args nil))
  ([args opts]
   (let [{:keys [exit] :as res} (shell/command args opts)]
     (if (not (zero? exit))
       (status/fatal (format "shell exited with %d for:\n %s" exit args) exit)
       res))))

;;
;; Tasks
;;

(defn clean []
  (status/line :info "Clean")
  (fs/delete-file-recursively ".cpcache" true)
  (fs/delete-file-recursively "classes" true)

  (.mkdirs (io/file "classes"))
  (.mkdirs (io/file "target"))
  (status/line :detail "all clean"))

(defn expose-api-to-sci []
  (status/line :info "Expose rewrite-cljc API to sci")
  (command ["clojure" "-A:sci-test-gen-publics"]))

(defn compute-classpath []
  (status/line :info "Compute classpath")
  (let [jdk-major-version (env/get-jdk-major-version)
        reflection-fix? (>= jdk-major-version 11)]
    (status/line :detail (str "JDK major version seems to be " jdk-major-version "; "
                              (if reflection-fix? "including" "excluding") " reflection fixes." ))
    (let [alias "-A:sci-test:native-image"
          alias (if reflection-fix? (str alias ":jdk11-reflect") alias)
          classpath (-> (command ["clojure" alias "-Spath"] {:out-to-string? true})
                        :out
                        string/trim)]
      (println "\nClasspath:")
      (println (str "- " (string/join "\n- " (fs/split-path-list classpath))))
      classpath)))

(defn aot-compile-sources [classpath]
  (status/line :info "AOT compile sources")
  (shell/command ["java"
                  "-Dclojure.compiler.direct-linking=true"
                  "-cp" classpath
                  "clojure.main"
                  "-e" "(compile 'sci-test.main)"]))

(defn generate-reflection-file [fname]
  (status/line :info "Generate reflection file for Graal native-image")
  (io/make-parents fname)
  (shell/command ["clojure" "-A:sci-test:gen-reflection" fname])
  (status/line :detail fname))

(defn run-native-image [{:keys [ :graal-native-image :graal-reflection-fname :target-exe :classpath :native-image-xmx]}]
  (status/line :info "Graal native-image compile AOT")
  (fs/delete-file-recursively target-exe true)
  (let [native-image-cmd
        [graal-native-image
         (str "-H:Name=" target-exe)
         "-H:+ReportExceptionStackTraces"
         "-J-Dclojure.spec.skip-macros=true"
         "-J-Dclojure.compiler.direct-linking=true"
         (str "-H:ReflectionConfigurationFiles=" graal-reflection-fname)
         "--initialize-at-run-time=java.lang.Math$RandomNumberGeneratorHolder"
         "--initialize-at-build-time"
         "-H:Log=registerResource:"
         "--enable-all-security-services"
         "--verbose"
         "--no-fallback"
         "--no-server"
         "--report-unsupported-elements-at-runtime"
         "-cp" (str classpath ":classes")
         (str "-J-Xmx" native-image-xmx)
         "sci_test.main"]
        time-cmd (case os
                   :mac ["command" "time" "-l"]
                   :unix ["command" "time" "-v"]
                   (status/fatal (str "I don't know how to time a command on " os) 1))]

    (command (concat time-cmd native-image-cmd))))

(defn -main [ & _args ]
  (let [native-image-xmx "3500m"
        graal-reflection-fname "target/native-image/reflection.json"
        target-exe "target/sci-test-rewrite-cljc"]
    (status/line :info "Creating native image for testing via sci")
    (status/line :detail "java -version" )
    (command ["java" "-version"])
    (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
    (let [graal-native-image (graal/find-graal-native-image)]
      (clean)
      (expose-api-to-sci)
      (let [classpath (compute-classpath)]
        (aot-compile-sources classpath)
        (generate-reflection-file graal-reflection-fname)
        (run-native-image {:graal-native-image graal-native-image
                           :graal-reflection-fname graal-reflection-fname
                           :target-exe target-exe
                           :classpath classpath
                           :native-image-xmx native-image-xmx})))
    (status/line :info "All done")
    (status/line :detail (format "built: %s, %d bytes" target-exe (.length (io/file target-exe))))))

(-main)
