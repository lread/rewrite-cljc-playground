(ns ^:no-doc rewrite-clj.impl.potemkin-clj
  (:require [rewrite-clj.impl.potemkin-helper :as helper]
            [clojure.pprint :as pprint]))

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
  (with-out-str (pprint/pprint o)))

;; TODO: is link-vars overkill for this proj? mebbe I don't understand it really
(defn link-vars
  "Makes sure that all changes to `src` are reflected in `dst`."
  [src dst]
  #_(add-watch src dst
    (fn [_ src old new]
      (alter-var-root dst (constantly @src))
      (alter-meta! dst merge (dissoc (meta src) :name)))))

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  ([sym]
   `(import-fn ~sym {}))
  ([sym opts]
   ;;(println "clj--import-fn FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF" sym)
   (let [vr (or (resolve sym) (throw (IllegalArgumentException. (str "Don't recognize " sym))))
         m (meta vr)
         n (:name m)
         protocol (:protocol m)
         m (helper/alter-meta m opts)
         n (helper/alter-sym n opts)]
     (when (:macro m)
       (throw (IllegalArgumentException.
               (str "Calling import-fn on a macro: " sym))))
     ;;(println "clj--import-fn pre do... vr" (pretty-str vr) )
     ;;(println "clj--import-fn pre do... dvr" (pretty-str (deref vr)) )
     ;;(println "clj--import-fn pre do... m" (pretty-str m))
     ;;(println "clj--import-fn pre do .. mvr" (pretty-str (meta vr)))
     ;;(println "clj--import-fn pre do... n" (pretty-str n))
     ;;(println "clj--import-fn pre do... o" (pretty-str opts))

     `(do
        (def ~(with-meta n {:protocol protocol}) (deref ~vr))
        (alter-meta! (var ~n) merge (dissoc '~m :name))
        ~vr))))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  ([sym]
   `(import-macro ~sym {}))
  ([sym opts]
   ;;(println "import-macro clj MMMMMMMMMMMMMMMMMMMMMMMMM" sym)
   (let [vr (or (resolve sym) (throw (IllegalArgumentException. (str "Don't recognize " sym))))
         m (meta vr)
         n (:name m)
         m (helper/alter-meta m opts)
         n (helper/alter-sym n opts)
         n (with-meta n {})]
     (when-not (:macro m)
       (throw (IllegalArgumentException.
               (str "Calling import-macro on a non-macro: " sym))))
     `(do
        #_(def ~(with-meta n (dissoc m :name))  ~vr)
        (def ~n ~(resolve sym))
        (alter-meta! (var ~n) merge (dissoc '~m :name))
        (.setMacro (var ~n))
        ~vr))))

;; TODO: unused methinks for this project...
;; TODO: untested
(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  ([sym]
   `(import-def ~sym {}))
  ([sym opts]
   ;;(println "import-def clj DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD" sym)
   (let [vr (or (resolve sym) (throw (IllegalArgumentException. (str "Don't recognize " sym))))
         m (meta vr)
         n (:name m)
         m (helper/alter-meta m opts)
         n (helper/alter-sym n opts)
         n (with-meta n (if (:dynamic m) {:dynamic true} {}))]
     `(do
        (def ~n @~vr)
        (alter-meta! (var ~n) merge (dissoc '~m :name))
        ~vr))))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [syms (helper/unravel-syms syms)]
    `(do
       ~@(map
          (fn [[sym opts]]
            (let [vr (resolve sym)
                  m (meta vr)]
              (cond
                (:macro m) `(import-macro ~sym ~opts)
                (:arglists m) `(import-fn ~sym ~opts)
                :else `(import-def ~sym ~opts))))
          syms))))

;; --- potemkin.types

(defmacro defprotocol+
  "A simpler version of 'potemkin.types/defprotocol+'."
  [name & body]
  (let [prev-body (-> name resolve meta :potemkin/body)]
    (when-not (= prev-body body)
      `(let [p# (defprotocol ~name ~@body)]
         (alter-meta! (resolve p#) assoc :potemkin/body '~body)
         p#))))
