(ns ^:no-doc rewrite-cljc.potemkin.clojure
  (:require [rewrite-cljc.potemkin.helper :as helper]))

;; Strongly based on code from:
;;
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

(defn- resolve-sym [sym]
  (or (resolve sym)
      (throw (ex-info (str "potemkin clj does not recognize symbol: " sym) {:symbol sym}))))

(defn- resolve-fn-location[var-meta]
  (if-let [p (:protocol var-meta)]
    (-> (meta p)
        (select-keys [:file :line])
        (merge var-meta))
    var-meta))

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  [src-sym target-name target-meta]
  (let [vr (resolve-sym src-sym)
        m (meta vr)
        m (resolve-fn-location m)
        new-meta (dissoc target-meta :name)
        protocol (:protocol m)]
    (when (:macro m)
      (throw (ex-info "potemkin clj cannot import-fn on a macro" {:symbol src-sym})))
    `(do
       (def ~(with-meta target-name (if protocol {:protocol protocol} {})) (deref ~vr))
       (alter-meta! (var ~target-name) merge '~new-meta)
       ~vr)))

(defmacro import-macro
  "Given a macro in another namespace, defines a macro with the same
   name in the current namespace.  Argument lists, doc-strings, and
   original line-numbers are preserved."
  [src-sym target-name target-meta]
  (let [vr (resolve-sym src-sym)
        m (meta vr)
        new-meta (dissoc target-meta :name)]
     (when-not (:macro m)
       (throw (ex-info "potemkin clj can only import-macro on macro" {:symbol src-sym})))
     `(do
        (def ~target-name (deref ~vr))
        (alter-meta! (var ~target-name) merge '~new-meta)
        (.setMacro (var ~target-name))
        ~vr)))

(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  [src-sym target-name target-meta]
  (let [vr (resolve-sym src-sym)
        m (meta vr)
        new-meta (dissoc target-meta :name)
        target-name (with-meta target-name (if (:dynamic m) {:dynamic true} {}))]
    `(do
       (def ~target-name @~vr)
       (alter-meta! (var ~target-name) merge '~new-meta)
       ~vr)))

(defmacro _import-var-data [import-data]
  (let [import-cmds (map
                     (fn [[sym type target-name new-meta]]
                       (case type
                         :macro `(import-macro ~sym ~target-name ~new-meta)
                         :fn    `(import-fn ~sym ~target-name ~new-meta)
                         :var   `(import-def ~sym ~target-name ~new-meta)))
                     import-data)]
    `(do ~@import-cmds)))


(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& raw-syms]
  (let [import-data (helper/syms->import-data raw-syms resolve-sym meta {})]
    `(_import-var-data ~import-data)))

(defmacro import-vars-with-mods
  "Imports a list of vars from other namespaces modifying imports via `opts`. "
  [opts & raw-syms]
  (let [import-data (helper/syms->import-data raw-syms resolve-sym meta opts)]
    `(_import-var-data ~import-data)))

;; --- potemkin.types

(defmacro defprotocol+
  "A simpler version of 'potemkin.types/defprotocol+'."
  [name & body]
  (let [prev-body (-> name resolve meta :potemkin/body)]
    (when-not (= prev-body body)
      `(let [p# (defprotocol ~name ~@body)]
         (alter-meta! (resolve p#) assoc :potemkin/body '~body)
         p#))))
