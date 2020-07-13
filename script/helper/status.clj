(ns helper.status)

(defn line [type msg]
  (let [fmt (case type
              :info "\n\u001B[42m \u001B[30;46m %s \u001B[42m \u001B[0m"
              :detail "%s"
              :error "\n\u001B[30;43m*\u001B[41m error: %s \u001B[43m*\u001B[0m"
              (throw (ex-info (format "unrecognized type: %s for status msg: %s" type msg) {})))]
    (println (format fmt msg))))

(defn fatal [msg exit-code]
  (line :error msg)
  (System/exit exit-code))
