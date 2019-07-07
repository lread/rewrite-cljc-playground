(ns ^:no-doc rewrite-clj.warning-handler
  (:require [cljs.analyzer :as ana]
            [clojure.java.io :as io]))
;;
;; The clojurescript compiler emits all kinds of useful warnings. Each warning type can be suppressed wholesale.
;;
;; A library continues to support and test its deprecated functions.
;; We want to know if we are calling a deprecated function we don't intend to, and suppress warnings for those
;; calls which are intentional. So wholesale suppression does not work for us here.
;;
;; This is a first shot at suppressing these types of warnings.
;; Ideally, I'd have metadata in the source to indicate the suppression, but in my explorations could not figure
;; out how to access such metadata, hence this somewhat clumsy configuration. Matching by exact line number
;; is especially brittle, but is a compromise for now.
;;
;; We are very serious people so we also want to fail on warning.
;; Ideally, we'd present all of our warnings and then fail the build, but I do not see a way to do this yet.
;; So, we fail on the first warning.

(def ignore-config
  {:fn-deprecated
   [{:file "test/rewrite_clj/regression_test.cljc"}
    {:file "test/rewrite_clj/examples/cljx_test.cljc"}
    {:file "test/rewrite_clj/zip/whitespace_test.cljc"}
    {:file "src/rewrite_clj/zip/base.cljc" :line 65}]})

(def fail-on-first-warning? true)

(defn- canonical-path [file]
  (.getCanonicalPath (io/file file)))

(defn- ignore-warning?[warning-type env extra]
  (when-let [files (warning-type ignore-config)]
    (let [source-info (ana/source-info env)
          source-file (canonical-path (:file source-info))
          source-line (:line source-info)
          result (first (filter #(let [ignore-file (canonical-path (:file %))
                                       at-line (:line %)]
                                   (or (and (nil? at-line)
                                            (= source-file ignore-file))
                                       (and (= source-file ignore-file)
                                            (= source-line at-line))))
                                files))]

      result)))

(defn suppressor [warning-type env extra]
  (when (not (ignore-warning? warning-type env extra))
    (when (warning-type ana/*cljs-warnings*)
      (ana/default-warning-handler warning-type env extra)
      (when fail-on-first-warning?
        (binding [*out* *err*]
          (println "FAILED build on first warnings"))
        (System/exit 1)))))
