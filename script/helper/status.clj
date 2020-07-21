(ns helper.status
  (:require [clojure.string :as string]))

(def ansi-codes {:inverse "7"

                 :fg-black "30"
                 :fg-red "31"
                 :fg-yellow "33"

                 :bg-red "41"
                 :bg-strong-red "101"
                 :bg-green "42"
                 :bg-strong-green "102"
                 :bg-yellow "43"
                 :bg-strong-yellow "103"
                 :bg-cyan "46"

                 :reset "0"})

(defn ansi-esc* [ & codes ]
  (str #_"ESC[" "\u001b[" (string/join ";" codes) "m"))

(defn ansi-esc [ & lookups ]
  (apply ansi-esc* (map #(get ansi-codes %) lookups)))

(def fmts {:info (str "\n"
                      (ansi-esc :bg-green :fg-black) " "
                      (ansi-esc :bg-cyan :fg-black) " %s "
                      (ansi-esc :bg-green :fg-black) " " (ansi-esc :reset))
           :detail "%s"
           :warn (str "\n"
                      (ansi-esc :bg-yellow :fg-black) "-"
                      (ansi-esc :bg-strong-yellow :fg-black) " warning: %s "
                      (ansi-esc :bg-yellow :fg-black) "-" (ansi-esc :reset))
           :error (str "\n"
                       (ansi-esc :bg-red :fg-black) "*"
                       (ansi-esc :bg-strong-red :fg-black) " error: %s "
                       (ansi-esc :bg-red :fg-black) "*" (ansi-esc :reset))})

(defn line [type msg]
  (if-let [fmt (get fmts type)]
    (println (format fmt msg))
    (throw (ex-info (format "unrecognized type: %s for status msg: %s" type msg) {}))))

(defn fatal [msg exit-code]
  (line :error msg)
  (System/exit exit-code))
