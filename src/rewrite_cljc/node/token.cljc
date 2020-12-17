(ns ^:no-doc rewrite-cljc.node.token
  (:require [rewrite-cljc.node.protocols :as node]))

#?(:clj (set! *warn-on-reflection* true))

;; ## Node

(defn- choose-qualifier [map-qualifier sym-qualifier]
  (when (not (and map-qualifier (= "_" (:prefix sym-qualifier))))
    (or sym-qualifier map-qualifier)))

(defn- symbol-qualifier [value]
  (when (qualified-symbol? value)
    {:prefix (namespace value)}))

;; A symbol is different than a keyword in that it can only be auto-resolve qualified by a namespaced map
(defn- symbol-sexpr [value map-qualifier {:keys [auto-resolve]}]
  (let [q (choose-qualifier map-qualifier (symbol-qualifier value))]
    (symbol (some-> (if (:auto-resolved? q)
                      ((or auto-resolve node/default-auto-resolve)
                       (or (some-> (:prefix q) symbol)
                           :current))
                      (:prefix q))
                    str)
            (name value))))

(defrecord TokenNode [value string-value]
  node/Node
  (tag [_n] :token)
  (node-type [_n] :token)
  (printable-only? [_n] false)
  (sexpr [_n] value)
  (sexpr [_n _opts] value)
  (length [_n] (count string-value))
  (string [_n] string-value)

  Object
  (toString [n]
    (node/string n)))

(defrecord SymbolNode [value string-value map-qualifier]
  node/Node
  (tag [_n] :token)
  (node-type [_n] :symbol)
  (printable-only? [_n] false)
  (sexpr [_n]
    (symbol-sexpr value map-qualifier {}))
  (sexpr [_n opts]
    (symbol-sexpr value map-qualifier opts))
  (length [_n] (count string-value))
  (string [_n] string-value)

  node/MapQualifiable
  (apply-map-context [n map-qualifier]
    (assoc n :map-qualifier map-qualifier))
  (clear-map-context [n]
    (assoc n :map-qualifier nil))

  Object
  (toString [this]
    (node/string this)))

(node/make-printable! TokenNode)
(node/make-printable! SymbolNode)

(defn symbol-node? [n]
  (and (= :token (node/tag n))
       (symbol? (:value n))))

;; ## Constructor

(defn token-node
  "Create node for an unspecified token of `value`."
  ([value]
   (token-node value (pr-str value)))
  ([value string-value]
    (if (symbol? value)
      (->SymbolNode value string-value nil)
      (->TokenNode value string-value))))


(defn token-node2
  "Create node for an unspecified token of `value`."
  ([value]
   (token-node value (pr-str value)))
  ([value string-value]
   (->TokenNode value string-value)))
