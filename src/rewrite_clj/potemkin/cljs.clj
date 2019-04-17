(ns ^:no-doc rewrite-clj.potemkin.cljs
 (:require [clojure.string :as string]
           [cljs.analyzer.api :as ana-api]
           [cljs.env :as env]
           [cljs.analyzer :as ana]
           [rewrite-clj.potemkin.helper :as helper]))

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

(defn resolved-meta
  "mimic what meta would return on a resolved sym in clj"
  [sym-analysis]
  (assoc (:meta sym-analysis)
         ;; name is not present under meta, add it to mimic meta
         :name (symbol (name (:name sym-analysis)))
         ;; doc is not always under meta (ex. for def)
         :doc (get-in sym-analysis [:meta :doc] (:doc sym-analysis))))

(defn resolve-sym [sym]
  (or (ana-api/resolve @env/*compiler* sym)
      (throw (ex-info "potemkin cljs does not recognize symbol" {:symbol 'sym}))))

(defn adjust-var-meta! [target-ns target-name src-sym]
  (let [src-ns (symbol (namespace src-sym))
        src-name (symbol (name src-sym))
        target-ns (symbol (str target-ns))
        src-data (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])]
    (and (or (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])
             (throw (ex-info "adjust-var-meta! did not find metadata for source symbol" {:ns src-ns :name src-name})))
         (or (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name])
             (throw (ex-info "adjust-var-meta! did not find metadata for target symbol" {:ns target-ns :name target-name})))
         (do
           (swap! env/*compiler*
                  update-in [::ana/namespaces target-ns :defs target-name]
                  merge (dissoc src-data :name :doc))
           (swap! env/*compiler*
                  update-in [::ana/namespaces target-ns :defs target-name :meta]
                  merge (dissoc (:meta src-data) :name :doc)))))
  nil)

(defmacro fixup-vars
  "We can't alter-meta! in cljs, some metadata needs to be changed in the compiler state"
  [target-ns & import-data]
  (doall
   (for [[src-sym target-name _] import-data]
     (adjust-var-meta! target-ns target-name src-sym)))
  nil)

(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  [src-sym target-name target-meta-changes]
  (let [vr (resolve-sym src-sym)
        m (resolved-meta vr)
        new-meta (-> m (merge target-meta-changes) (dissoc :name))]
    (when (:macro m)
      (throw (ex-info "potemkin cljs cannot import-fn on a macro" {:symbol src-sym})))
    `(def ~(with-meta target-name new-meta) ~(:name vr))))

(defmacro import-macro
  "Not supported for cljs"
  [src-sym target-name target-meta-changes]
  (throw (ex-info "potemkin cljs cannot import macros, do macro importing via potemkin clj" {:symbol src-sym})))

(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  [src-sym target-name target-meta-changes]
  (let [vr (resolve-sym src-sym)
        m (resolved-meta vr)
        new-meta (-> m (merge target-meta-changes) (dissoc :name))]
    `(def ~(with-meta target-name new-meta) ~(:name vr))))

(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& raw-syms]
  (let [syms (helper/unravel-syms raw-syms)
        import-data (map
                     (fn [[sym opts]]
                       (let [vr (resolve-sym sym)
                             m (resolved-meta vr)
                             n (:name m)]
                         [sym (helper/new-name n opts) (helper/new-meta m opts)]))
                     syms)
        import-cmds (map
                     (fn [[sym new-name new-meta]]
                       (let [vr (resolve-sym sym)
                             m (resolved-meta vr)]
                         (cond
                           (:macro m)    `(import-macro ~sym ~new-name ~new-meta)
                           (:arglists m) `(import-fn ~sym ~new-name ~new-meta)
                           :else         `(import-def ~sym ~new-name ~new-meta))))
                     import-data)
        cmds (concat import-cmds [`(fixup-vars ~*ns* ~@import-data)])]
    `(do ~@cmds)))


;; --- potemkin.types

(defmacro defprotocol+
  "Currently a no-op for cljs."
  [name & body]
  `(defprotocol ~name ~@body))
