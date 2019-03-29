(ns ^:no-doc rewrite-clj.impl.potemkin2-cljs
 (:require [clojure.string :as string]
           [cljs.analyzer.api :as ana-api]
           [cljs.util :as util]
           [rewrite-clj.impl.potemkin-helper :as helper]))
;; TODO: could not, for the life of me, figure out how to make this cljc. So we have cljs version and a cljc versin which are quite similar

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
  ;;(util/debug-prn "cljs resolving " fully-qualified-sym)
  (let [vr (ana-api/ns-resolve (symbol (namespace fully-qualified-sym))
                               (symbol (name fully-qualified-sym)))]
      ;;(util/debug-prn "-vr-->" (pr-str vr))
      ;;(util/debug-prn "-mt-->" (pr-str (resolved-meta vr)) "\n")
      vr))

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  ([sym]
     `(import-fn ~sym {}))
  ([sym opts]
   ;;(util/debug-prn "import-fn cljs======>" sym)
   (let [vr (resolve-sym sym)
         m (resolved-meta vr)
         n (:name m)
         m (helper/alter-meta m opts)
         n (helper/alter-sym n opts)]
     (when-not vr
       (throw (ex-info (str "Don't recognize " sym) {})))
     (when (:macro m)
       (throw (ex-info (str "Calling import-fn on a macro: " sym) {})))
     ;;(util/debug-prn "import-fn pre do... vr" (pretty-str vr) )
     ;;(util/debug-prn "import-fn pre do... m" (pretty-str m) )
     ;;(util/debug-prn "import-fn pre do... n" n )
     ;;(util/debug-prn "import-fn pre do... (var n)" (var n))
     `(do
        (def ~(with-meta n (dissoc m :name)) ~(:name vr))))))

;; TODO: will this import cljc macros twice? if used?  am importing macros via clj version now.
(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  ([sym]
     `(import-macro ~sym {}))
  ([sym opts]
   ;;(util/debug-prn "import-macro cljs" sym)
   (let [vr (resolve-sym sym)
         m (resolved-meta vr)
         n (:name m)
         m (helper/alter-meta m opts)
         n (helper/alter-sym n opts)
         n (with-meta n {})]
     (when-not vr
       (throw (ex-info (str "Don't recognize " sym) {})))
     (when-not (:macro m)
       (throw (ex-info (str "Calling import-macro on a non-macro: " sym) {})))
     `(do
        (def ~n ~(resolve-sym sym))
        (alter-meta! (var ~n) merge (dissoc ~m :name))
        (.setMacro (var ~n))))))

;; TODO: I don't expect our project will make use of this one...
;; TODO: untested
(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  ([sym]
     `(import-def ~sym {}))
  ([sym opts]
   ;;(util/debug-prn "import-def" sym)
   (let [vr (resolve-sym sym)
         m (resolved-meta vr)
         n (:name m)
         m (helper/alter-meta m opts)
         n (helper/alter-sym n opts)
         n (with-meta n (if (:dynamic m) {:dynamic true} {}))]
     (when-not vr
       (throw (ex-info (str "Don't recognize " sym) {})))
     `(do
        (def ~n @~vr)
        (alter-meta! (var ~n) merge (dissoc ~m :name))))))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& syms]
  (let [syms (helper/unravel-syms syms)]
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
