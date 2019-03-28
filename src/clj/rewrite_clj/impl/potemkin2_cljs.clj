(ns ^:no-doc rewrite-clj.impl.potemkin2-cljs
 (:require [clojure.string :as string]
           [cljs.analyzer.api :as ana-api]
           [cljs.util :as util]))
;; TODO: could not, for the life of me, figure out how to make this cljc.

;; --- copied from ztellman/potemkin
;;
;; Copyright (c) 2013 Zachary Tellman
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files  (the  "Software"), to
;; deal in the Software without restriction, including without limitation the
;; rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
;; sell copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in
;; all copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED  "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
;; FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
;; IN THE SOFTWARE.
;;
;; ---

;; --- potemkin.namespaces

(defn- pretty-str [o]
  (with-out-str (clojure.pprint/pprint o)))

;; TODO: assuming link-vars is only possible for clj - not even sure we want/need this at all
(defn link-vars
  "Makes sure that all changes to `src` are reflected in `dst`."
  [src dst]
     #_(add-watch src dst
                (fn [_ src old new]
                  (alter-var-root dst (constantly @src))
                  (alter-meta! dst merge (dissoc (meta src) :name)))))

(defn resolved-meta [resolved-sym]
  (assoc (:meta resolved-sym) :name (symbol (name (:name resolved-sym)))))

(defn resolve-sym [fully-qualified-sym]
  (util/debug-prn "cljs resolving " fully-qualified-sym)
  (let [vr (ana-api/ns-resolve (symbol (namespace fully-qualified-sym))
                               (symbol (name fully-qualified-sym)))]
      ;;(util/debug-prn "-vr-->" (pr-str vr))
      ;;(util/debug-prn "-mt-->" (pr-str (resolved-meta vr)) "\n")
      vr))

(defn alter-sym [orig-sym opts]
  (if-let [sym-pattern (:sym-to-pattern opts)]
    (symbol (string/replace sym-pattern #"@@orig-sym@@" (str orig-sym)))
    orig-sym))

(defn alter-meta [orig-meta opts]
  (util/debug-prn "alter-meta... orig-meta" (pretty-str orig-meta) )
  (util/debug-prn "alter-meta... opts" (pretty-str opts) )
  (if-let [doc-pattern (:doc-to-pattern opts)]
    (update orig-meta
            :doc
            (fn [orig-doc]
              (-> doc-pattern
                  (string/replace #"@@orig-sym@@" (str (:name orig-meta)))
                  (string/replace #"@@orig-doc@@" (or orig-doc "")))))
    orig-meta))


(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  ([sym]
     `(import-fn ~sym {}))
  ([sym opts]
   ;;(util/debug-prn "\n\nimport-fn======>" sym)
   (let [vr (resolve-sym sym)
         m (resolved-meta vr)
         n (:name m)
         m (alter-meta m opts)
         n (alter-sym n opts)]
     (when-not vr
       (throw (ex-info (str "Don't recognize " sym) {})))
     (when (:macro m)
       (throw (ex-info (str "Calling import-fn on a macro: " sym) {})))
     ;;(util/debug-prn "import-fn pre do... vr" (pretty-str vr) )
     ;;(util/debug-prn "import-fn pre do... m" (pretty-str m) )
     ;;(util/debug-prn "import-fn pre do... n" n )
     ;;(util/debug-prn "import-fn pre do... p" protocol)
     ;;(util/debug-prn "import-fn pre do... (var n)" (var n))
     `(do
        (def ~(with-meta n (dissoc m :name)) ~(:name vr))
        #_(alter-meta! (var ~n) merge (dissoc ~m :name))))))

;; TODO: will this import cljc macros twice?
(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  ([sym]
     `(import-macro ~sym {}))
  ([sym opts]
   (util/debug-prn "import-macro" sym)
   (let [vr (resolve-sym sym)
         m (resolved-meta vr)
         n (with-meta (:name m) {})]
     (when-not vr
       (throw (ex-info (str "Don't recognize " sym) {})))
     (when-not (:macro m)
       (throw (ex-info (str "Calling import-macro on a non-macro: " sym) {})))
     `(do
        (def ~n ~(resolve-sym sym))
        (alter-meta! (var ~n) merge (dissoc (resolved-meta ~vr) :name))
        (.setMacro (var ~n))
        (link-vars ~vr (var ~n))
        ~vr))))

;; TODO: I don't expect our project will make use of this one...
(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  ([sym]
     `(import-def ~sym {}))
  ([sym opts]
   (util/debug-prn "import-def" sym)
   (let [vr (resolve-sym sym)
         m (resolved-meta vr)
         n (:name m)
         n (with-meta n (if (:dynamic m) {:dynamic true} {}))
         ]
     (when-not vr
       (throw (ex-info (str "Don't recognize " sym) {})))
     `(do
        (def ~n @~vr)
        (alter-meta! (var ~n) merge (dissoc (resolved-meta ~vr) :name))
        (link-vars ~vr (var ~n))
        ~vr))))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [unravel (fn unravel [x]
                  (loop [acc []
                         r x
                         cur-opts {}]
                    (if-let [n (first r)]
                      (cond
                        (sequential? n)
                        (recur (apply conj acc (map #(vector (symbol
                                                              (str (first n)
                                                                   (when-let [ns (namespace %)]
                                                                     (str "." ns)))
                                                              (name %))
                                                             cur-opts)
                                                    (rest n)))
                               (rest r)
                               cur-opts)

                        (map? n)
                        (recur acc (rest r) n)

                        :else
                        (recur (conj acc [n cur-opts]) (rest r) cur-opts))
                      acc)))
        syms (unravel syms)]
    `(do
       ~@(map
          (fn [[sym opts]]
            (let [vr (resolve-sym sym)
                  m (resolved-meta vr)]
              (cond
               (:macro m) `(import-macro ~sym ~opts)
               (:arglists m) `(import-fn ~sym ~opts)
               :else `(import-def ~sym ~opts))))
          syms))))



;; --- potemkin.types

;; TODO: assuming this has no value for cljs
(defmacro defprotocol+
  "A simpler version of 'potemkin.types/defprotocol+'."
  [name & body]
  `(defprotocol ~name ~@body))


(comment
  (defn unravel [x]
    (if (sequential? x)
      (->> x
           rest
           (mapcat unravel)
           (map
            #(symbol
              (str (first x)
                   (when-let [n (namespace %)]
                     (str "." n)))
              (name %))))
      [x]))

  (defn unravel2
    ([x] (unravel2 x nil))
    ([x cur-opts]
     (cond
       (sequential? x)
       (->> x
            rest
            (mapcat unravel2 cur-opts)
            (map
             #(symbol
               (str (first x)
                    (when-let [n (namespace %)]
                      (str "." n)))
               [(name %) cur-opts])))
       (map? x)
       (unravel2 nil x)
       :else
       [x cur-opts])))

  (defn unravel3 [x]
    (loop [acc []
           r x
           cur-opts {}]
      (if-let [n (first r)]
        (cond
          (sequential? n)
          (recur (apply conj acc (map #(vector (symbol
                                                (str (first n)
                                                     (when-let [ns (namespace %)]
                                                       (str "." ns)))
                                                (name %))
                                               cur-opts)
                                      (rest n)))
                 (rest r)
                 cur-opts)

          (map? n)
          (recur acc (rest r) n)

          :else
          (recur (conj acc [n cur-opts]) (rest r) cur-opts))
        acc)))

  (require '[clojure.string])
  (unravel3 (list ['clojure.string 'join 'split]))
  (unravel3 (list {:opts 1} ['clojure.string 'join 'split]))
  (unravel3 (list 'clojure.string/escape
                  {:opts 1}
                  ['clojure.string 'join 'split]
                  ['clojure.string 'index-of]
                  {:opts 2}
                  ['clojure.string 'trim]
                  {}
                  ['clojure.string 'capitalize]))
)
