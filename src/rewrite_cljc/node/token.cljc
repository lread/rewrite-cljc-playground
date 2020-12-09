(ns ^:no-doc rewrite-cljc.node.token
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

;; TODO: some nsmap code is common to keyword
;; TODO: consider splitting out SymbolNode to its own record? After that could add a NamespacedMapable protocol...

;; A symbol is different than a keyword in that it can only be auto-resolve qualified by a namespaced map
(defn- choose-qualifier [map-qualifier sym-qualifier]
  (when (not (and map-qualifier (= "_" (:prefix sym-qualifier))))
    (or sym-qualifier map-qualifier)))

(defn- symbol-qualifier [value]
  (when (qualified-symbol? value)
    {:prefix (namespace value)}))

(defn- symbol-sexpr [value map-qualifier {:keys [auto-resolve]}]
  (let [q (choose-qualifier map-qualifier (symbol-qualifier value))]
    (symbol (some-> (if (:auto-resolved? q)
                      ((or auto-resolve node/default-auto-resolve)
                       (or (some-> (:prefix q) symbol)
                           :current))
                      (:prefix q))
                    str)
            (name value))))

(defn- token-sexpr [value map-qualifier opts]
  (if (symbol? value)
    (symbol-sexpr value map-qualifier opts)
    value))

(defrecord TokenNode [value string-value map-qualifier]
  node/Node
  (tag [_n] :token)
  (printable-only? [_n] false)
  (sexpr [_n]
    (token-sexpr value map-qualifier {}))
  (sexpr [_n opts]
    (token-sexpr value map-qualifier opts))
  (length [_n] (count string-value))
  (string [_n] string-value)

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! TokenNode)

(defn symbol-node? [n]
  (and (= :token (node/tag n))
       (symbol? (:value n))))

;; ## Constructor

(defn token-node
  "Create node for an unspecified token of `value`."
  ([value]
   (token-node value (pr-str value)))
  ([value string-value]
  (->TokenNode value string-value nil)))
