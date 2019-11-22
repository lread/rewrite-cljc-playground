(ns clj-graal.gen-vars-run-directly
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.namespace.find :as find]))

(defn- find-test-nses[]
  (->> (find/find-namespaces [(io/file "test")] find/clj)
       (filter #(re-matches #".*-test" (str %)))))

(defn- find-test-vars[test-nses]
  (->> test-nses
       (mapcat (fn [ns]
                 (require ns)
                 (->> (the-ns ns)
                      ns-publics
                      vals
                      (filter #(:test (meta %))))))))

(defn- content[nses vars]
  (-> (slurp (io/resource "clj_graal/direct_runner.clj.template"))
      (.replace "#_@TEST_NSES_HERE" (string/join "\n" nses))
      (.replace "#_@TEST_FNS_HERE" (string/join "\n"
                                                (map (fn [ns]
                                                       (str
                                                        "(println \"running: " (symbol ns) " \")\n"
                                                        "(" (symbol ns) ")"))
                                                     vars)))))

(defn generate-test-runner[dest-src-dir]
  (let [dir (io/file dest-src-dir "clj_graal")
        nses (find-test-nses)
        vars (find-test-vars nses)]
    (.mkdirs dir)
    (spit (io/file dir "test_runner.clj")
          (str ";; auto-generated test runner to support running of tests under graal\n"
               (content nses vars)))))

(defn -main [dest-dir]
  (generate-test-runner dest-dir))

(comment
  (symbol #'io/file)

  (find-test-vars (find-test-nses)))
