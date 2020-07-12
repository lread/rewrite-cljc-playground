#!/usr/bin/env bb

(ns sci-test-gen-native-image
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]))

(import '[java.lang ProcessBuilder$Redirect])

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp #(re-find %1 %2) os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))

(def os (get-os))

(defn status-line [type msg]
  (let [fmt (case type
              :info "\n\u001B[42m \u001B[30;46m %s \u001B[42m \u001B[0m"
              :detail "%s"
              :error "\n\u001B[30;43m*\u001B[41m error: %s \u001B[43m*\u001B[0m"
              (throw (ex-info (format "unrecognized type: %s for status msg: %s" type msg) {})))]
    (println (format fmt msg))))

(defn fatal-error [msg]
  (status-line :error msg)
  (System/exit 1))

(defn at-path [path prog-name]
  (let [f (io/file path prog-name)]
    (when (and (.isFile f) (.canExecute f))
      (str (.getAbsolutePath f)))))

(defn delete-file-recursively
  [f & [silently]]
  (let [f (io/file f)]
    (when (.isDirectory f)
      (doseq [child (.listFiles f)]
        (delete-file-recursively child silently)))
    (io/delete-file f silently)))

(defn split-path-list [path-list]
  (string/split path-list
                (re-pattern (str "\\" java.io.File/pathSeparator))) )

(defn on-path [prog-name]
  (first (keep identity
               (map #(at-path % prog-name)
                    (split-path-list (System/getenv "PATH"))))))

(defn shell-command
  "Executes shell command. Exits script when the shell-command has a non-zero exit code, propagating it.
  Accepts the following options:
  `:input`: instead of reading from stdin, read from this string.
  `:to-string?`: instead of writing to stdout, write to a string and return it."
  ([args] (shell-command args nil))
  ([args {:keys [:input :to-string?]}]
   (let [args (mapv str args)
         pb (cond-> (-> (ProcessBuilder. ^java.util.List args)
                        (.redirectError ProcessBuilder$Redirect/INHERIT))
              (not to-string?) (.redirectOutput ProcessBuilder$Redirect/INHERIT)
              (not input) (.redirectInput ProcessBuilder$Redirect/INHERIT))
         proc (.start pb)]
     (when input
       (with-open [w (io/writer (.getOutputStream proc))]
         (binding [*out* w]
           (print input)
           (flush))))
     (let [string-out
           (when to-string?
             (let [sw (java.io.StringWriter.)]
               (with-open [w (io/reader (.getInputStream proc))]
                 (io/copy w sw))
               (str sw)))
           exit-code (.waitFor proc)]
       (when-not (zero? exit-code)
         (fatal-error (format "shell exited with %d for:\n %s" exit-code args)))
       string-out))))

(defn find-graal-prog [prog-name]
  (or (on-path prog-name)
      (at-path (str (io/file (System/getenv "JAVA_HOME") "bin")) prog-name)
      (at-path (str (io/file (System/getenv "GRAALVM_HOME") "bin")) prog-name)))

;;
;; Tasks
;;

(defn find-graal-native-image []
  (status-line :info "Locate GraalVM native-image")
  (let [res
        (or (find-graal-prog "native-image")
            (if-let [gu (find-graal-prog "gu")]
              (do
                (status-line :detail "GraalVM native-image not found, attempting install")
                (shell-command [gu "install" "native-image"])
                (or (find-graal-prog "native-image")
                    (fatal-error "failed to install GraalVM native-image, check your GraalVM installation")))
              (fatal-error "GraalVM native image not found nor its installer, check your GraalVM installation")))]
    (status-line :detail res)
    res))

(defn clean []
  (status-line :info "Clean")
  (delete-file-recursively ".cpcache" true)
  (delete-file-recursively "classes" true)

  (.mkdirs (io/file "classes"))
  (.mkdirs (io/file "target"))
  (status-line :detail "all clean"))

(defn expose-api-to-sci []
  (status-line :info "Expose rewrite-cljc API to sci")
  (shell-command ["clojure" "-A:sci-test-gen-publics"]))

(defn compute-classpath []
  (status-line :info "Compute classpath")
  (let [classpath (-> (shell-command ["clojure" "-A:sci-test:native-image:jdk11-reflect" "-Spath"] {:to-string? true})
                      string/trim)]
    (println (str "- " (string/join "\n- " (split-path-list classpath))))
    classpath))

(defn aot-compile-sources [classpath]
  (status-line :info "AOT compile sources")
  (shell-command ["java"
                  "-Dclojure.compiler.direct-linking=true"
                  "-cp" classpath
                  "clojure.main"
                  "-e" "(compile 'sci-test.main)"]))

(defn generate-reflection-file [fname]
  (status-line :info "Generate reflection file for Graal native-image")
  (io/make-parents fname)
  (shell-command ["clojure" "-A:sci-test:gen-reflection" fname])
  (status-line :detail fname))

(defn run-native-image [{:keys [ :graal-native-image :graal-reflection-fname :target-exe :classpath :native-image-xmx]}]
  (status-line :info "Graal native-image compile AOT")
  (delete-file-recursively target-exe true)
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
                   (fatal-error (str "I don't know how to time a command on " os)))]

    (shell-command (concat time-cmd native-image-cmd))))

(defn -main [ & _args ]
  (let [native-image-xmx "3500m"
        graal-reflection-fname "target/native-image/reflection.json"
        target-exe "target/sci-test-rewrite-cljc"]
    (status-line :info "Creating native image")
    (status-line :detail "java --version" )
    (shell-command ["java" "--version"])
    (status-line :detail (str "\nnative-image max memory: " native-image-xmx))
    (let [graal-native-image (find-graal-native-image)]
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
    (status-line :info "All done")
    (status-line :detail (format "built: %s, %d bytes" target-exe (.length (io/file target-exe))))))

(-main)
