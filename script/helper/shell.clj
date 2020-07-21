(ns helper.shell
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [helper.status :as status]))

(import '[java.lang ProcessBuilder$Redirect])

(defn get-os []
  (let [os-name (string/lower-case (System/getProperty "os.name"))]
    (condp #(re-find %1 %2) os-name
      #"win" :win
      #"mac" :mac
      #"(nix|nux|aix)" :unix
      #"sunos" :solaris
      :unknown)))

(defn- escape-double-quote [s]
  (string/replace s "\"" "\\\\\\\""))

(defn- escape-args
  "Escape rules are, to me, confusing for Windows: https://stackoverflow.com/a/20009602"
  [args]
  (if (= :win (get-os))
    (map escape-double-quote args)
    args))

(defn command-no-exit
  "Executes shell command. Exits script when the shell-command has a non-zero exit code, propagating it.

  Returns map of:
  :exit => sub-process exit code, will be 0 unless non-fatal option used
  :err  => sub-process stdout, will be nil unless stdout-to-string option used
  :out  => sub-process stderr, will be nil unless stderr-to-string option used

  Accepts the following options:
  `:input`: instead of reading from stdin, read from this string.
  `:out-to-string?`: instead of writing to stdout, write to a string and return it.
  `:err-to-string?`: instead of writing to stderr, write to a string and return in"
  ([args] (command-no-exit args nil))
  ([args {:keys [:input :out-to-string? :err-to-string?]}]
   (let [args (mapv str args)
         args (escape-args args)
         pb (cond-> (ProcessBuilder. ^java.util.List args)
              (not err-to-string?) (.redirectError ProcessBuilder$Redirect/INHERIT)
              (not out-to-string?) (.redirectOutput ProcessBuilder$Redirect/INHERIT)
              (not input) (.redirectInput ProcessBuilder$Redirect/INHERIT))
         proc (.start pb)]
     (when input
       (with-open [w (io/writer (.getOutputStream proc))]
         (binding [*out* w]
           (print input)
           (flush))))
     (let [string-out
           (when out-to-string?
             (let [sw (java.io.StringWriter.)]
               (with-open [w (io/reader (.getInputStream proc))]
                 (io/copy w sw))
               (str sw)))
           string-err
           (when err-to-string?
             (let [sw (java.io.StringWriter.)]
               (with-open [w (io/reader (.getErrorStream proc))]
                 (io/copy w sw))
               (str sw)))
           exit-code (.waitFor proc)]
       {:exit exit-code
        :out string-out
        :err string-err}))))

(defn command
  ([args] (command args nil))
  ([args opts]
   (let [{:keys [exit] :as res} (command-no-exit args opts)]
     (if (not (zero? exit))
       (status/fatal (format "shell exited with %d for:\n %s"
                             exit
                             (with-out-str (pprint/pprint args)))
                     exit)
       res))))
