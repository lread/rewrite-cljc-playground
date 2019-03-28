(ns ^:no-doc rewrite-clj.impl.potemkin2
  #?(:clj
     (:require [net.cgrand.macrovich :as macros]
               [cljs.util :as ut])
     :cljs
     (:require [cljs.analyzer.api :as ana-api]
               [cljs.util :as ut]))
  #?(:cljs
     (:require-macros [net.cgrand.macrovich :as macros]
                      [rewrite-clj.impl.potemkin :refer [import-vars]])))

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

;; TODO: assuming link-vars is only possible for clj
(defn link-vars
  "Makes sure that all changes to `src` are reflected in `dst`."
  [src dst]
  #?(:clj
     (add-watch src dst
                (fn [_ src old new]
                  (alter-var-root dst (constantly @src))
                  (alter-meta! dst merge (dissoc (meta src) :name))))))

(defn resolved-meta [resolved-sym]
  #?(:clj (meta resolved-sym)
     :cljs (assoc (:meta resolved-sym) :name (symbol (name (:name resolved-sym))))))

(defn resolve-sym [fully-qualified-sym]
  (ut/debug-prn "cljc resolving " fully-qualified-sym)
  #?(:clj (do (ut/debug-prn "clj resolve" fully-qualified-sym )
              (let [vr (resolve fully-qualified-sym)]
                (ut/debug-prn "clj:" (pr-str vr))
                vr))
     :cljs
     (do
       (ut/debug-prn "cljs resolving " fully-qualified-sym)
       (let [vr (ana-api/ns-resolve (symbol (namespace fully-qualified-sym))
                                    (symbol (name fully-qualified-sym)))]
         (do
           (ut/debug-prn "---\n")
           (ut/debug-prn vr)
           (ut/debug-prn "-2-\n")
           (ut/debug-prn (resolved-meta vr))
           vr)))))

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  ([sym]
     `(import-fn ~sym nil))
  ([sym new-name]
     (let [vr (resolve-sym sym)
           m (resolved-meta vr)
           ;; TODO: does cljs include :name in its meta?
           n (or new-name (:name m))
           ;; TODO was is protocol for?
           protocol (:protocol m)]
       (when-not vr
         (throw (ex-info (str "Don't recognize " sym) {})))
       (when (:macro m)
         (throw (ex-info (str "Calling import-fn on a macro: " sym) {})))

       `(do
          (def ~(with-meta n {:protocol protocol}) (deref ~vr))
          (alter-meta! (var ~n) merge (dissoc (resolved-meta ~vr) :name))
          (link-vars ~vr (var ~n))
          ~vr))))

;; TODO: will this import cljc macros twice?
(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  ([sym]
     `(import-macro ~sym nil))
  ([sym new-name]
     (let [vr (resolve-sym sym)
           m (resolved-meta vr)
           n (or new-name (with-meta (:name m) {}))]
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

(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  ([sym]
     `(import-def ~sym nil))
  ([sym new-name]
     (let [vr (resolve-sym sym)
           m (resolved-meta vr)
           n (or new-name (:name m))
           ;;n (with-meta n (if (:dynamic m) {:dynamic true} {}))
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
        syms (mapcat unravel syms)]
    `(do
       ~@(map
          (fn [sym]
            (let [vr (resolve-sym sym)
                  m (resolved-meta vr)]
              (cond
               (:macro m) `(import-macro ~sym)
               (:arglists m) `(import-fn ~sym)
               :else `(import-def ~sym))))
          syms))))

;; --- potemkin.types

;; TODO: assuming this has no value for cljs
(defmacro defprotocol+
  "A simpler version of 'potemkin.types/defprotocol+'."
  [name & body]
  (macros/case :clj (let [prev-body (-> name resolve meta :potemkin/body)]
                      (when-not (= prev-body body)
                        `(let [p# (defprotocol ~name ~@body)]
                           (alter-meta! (resolve p#) assoc :potemkin/body '~body)
                           p#)))
               :cljs `(defprotocol ~name ~@body)))


(comment
 (require-macros '[rewrite-clj.impl.potemkin2 :refer [defprotocol+ import-fn]])
 #_(defprotocol+ Player
   (choose [p])
   (update-strategy [p me you]))

 (require '[clojure.string])

 (resolve-sym 'clojure.string/join)
 (macroexpand-1 '(int 5))
 (macroexpand-1 '(import-fn clojure.string/join))
 (macroexpand-1 '(import-fn clojure.string/join join2))
 (import-fn cljs.string/join join2)

 41
 )
