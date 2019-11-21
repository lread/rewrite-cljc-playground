(ns clj-graal.gen-by-discovery-test-runner
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [clojure.tools.namespace.find :as find]))

(defn- test-nses[]
  (->> (find/find-namespaces [(io/file "test")] find/clj)
       (filter #(re-matches #".*-test" (str %)))))

(defn- content[nses]
  (let [runner-code  `((~'ns clj-graal.test-runner
                        (:gen-class)
                        (:require ~@(concat ['[clojure.test :as t]]
                                             nses)))
                       (~'defn ~'-main [& ~'_args]
                        (~'println "clojure version" (~'clojure-version))
                        (~'println "java version" (~'System/getProperty "java.version"))
                        (~'println "running native?"
                         (~'= "executable" (~'System/getProperty "org.graalvm.nativeimage.kind")))
                        (~'let [{:keys [~'fail ~'error]} (~'apply t/run-tests (quote ~nses))]
                          (~'System/exit (if (~'zero? (~'+ ~'fail ~'error)) 0 1)))))]
    (->> (macroexpand-1 runner-code)
         (map #(with-out-str (pprint/pprint %)))
         (string/join "\n"))))

(defn generate-test-runner[dest-src-dir]
  (let [dir (io/file dest-src-dir "clj_graal")]
    (.mkdirs dir)
    (spit (io/file dir "test_runner.clj")
          (str ";; auto-generated test runner to support running of tests under graal\n"
               (content (test-nses))))))

(defn -main [dest-dir]
  (generate-test-runner dest-dir))
