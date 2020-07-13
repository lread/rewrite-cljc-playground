#!/usr/bin/env bb

(ns sci-test-gen-native-image
  (:require
   [babashka.classpath :as cp]
   [clojure.java.io :as io]))

(cp/add-classpath "./script")
(require '[helper.status :as status]
         '[helper.shell :as shell]
         '[helper.graal :as graal])

(defn expose-api-to-sci []
  (status/line :info "Expose rewrite-cljc API to sci")
  (shell/command ["clojure" "-A:script" "-m" "sci-test-gen-publics"]))

(defn generate-reflection-file [fname]
  (status/line :info "Generate reflection file for Graal native-image")
  (io/make-parents fname)
  (shell/command ["clojure" "-A:sci-test:gen-reflection" fname])
  (status/line :detail fname))

(defn -main [ & _args ]
  (let [native-image-xmx "3500m"
        graal-reflection-fname "target/native-image/reflection.json"
        target-exe "target/sci-test-rewrite-cljc"]
    (status/line :info "Creating native image for testing via sci")
    (status/line :detail "java -version" )
    (shell/command ["java" "-version"])
    (status/line :detail (str "\nnative-image max memory: " native-image-xmx))
    (let [graal-native-image (graal/find-graal-native-image)]
      (graal/clean)
      (expose-api-to-sci)
      (let [classpath (graal/compute-classpath "sci-test:native-image"
                                               "jdk11-reflect:jdk11-reflect-sci")]
        (graal/aot-compile-sources classpath "sci-test.main")
        (generate-reflection-file graal-reflection-fname)
        (graal/run-native-image {:graal-native-image graal-native-image
                                 :graal-reflection-fname graal-reflection-fname
                                 :target-exe target-exe
                                 :classpath classpath
                                 :native-image-xmx native-image-xmx
                                 :entry-class "sci_test.main"})))
    (status/line :info "All done")
    (status/line :detail (format "built: %s, %d bytes"
                                 target-exe (.length (io/file target-exe))))))
(-main)
