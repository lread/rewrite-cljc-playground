(ns ^:no-doc rewrite-clj.impl.potemkin.clojurescript
 (:require [clojure.string :as string]
           [cljs.analyzer.api :as ana-api]
           [cljs.env :as env]
           [cljs.analyzer :as ana]
           [cljs.util :as util]
           [rewrite-clj.impl.potemkin.helper :as helper]))
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

(defn resolved-meta
  "mimic what meta would return on a resolved sym in clj"
  [sym-analysis]
  (assoc (:meta sym-analysis)
         ;; name is not present under meta, add it to mimic meta
         :name (symbol (name (:name sym-analysis)))
         ;; doc is not always under meta (ex. for def)
         :doc (get-in sym-analysis [:meta :doc] (:doc sym-analysis))))

(defn resolve-sym [sym]
  (ana-api/resolve @env/*compiler* sym))

(defn adjust-var-meta! [target-ns target-name src-sym]
  ;; (util/debug-prn "!!altering" target-ns target-name "from" src-sym)
  (let [src-ns (symbol (namespace src-sym))
        src-name (symbol (name src-sym))
        target-ns (symbol (str target-ns))
        src-data (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])]
    ;; (util/debug-prn "  !src-ns" src-ns)
    ;; (util/debug-prn "  !src-name" src-name)
    ;; (util/debug-prn "  !target-name" target-name)
    ;; (util/debug-prn "  !target-ns" target-ns)
    ;; (util/debug-prn "  ---src dump-----")
    ;; (util/debug-prn (pretty-str src-data))
    ;; (util/debug-prn "  ---pre---copy---")
    ;; (util/debug-prn "  !src      :file" (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name :meta :file]))
    ;; (util/debug-prn "  !src meta :file" (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name :file]))
    ;; (util/debug-prn "  !src      :line" (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name :meta :line]))
    ;; (util/debug-prn "  !src meta :line" (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name :line]))
    ;; (util/debug-prn "  !target      :file" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :file]))
    ;; (util/debug-prn "  !target meta :file" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :meta :file]))
    ;; (util/debug-prn "  !target      :line" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :line]))
    ;; (util/debug-prn "  !target meta :line" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :meta :line]))
    (and (or (get-in @env/*compiler* [::ana/namespaces src-ns :defs src-name])
             (throw (ex-info (str "adjust-var-meta! did not find source " src-ns src-name) {})))
         (or (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name])
             (throw (ex-info (str "adjust-var-meta! did not find target " target-ns target-name) {})))
         (do
           (swap! env/*compiler*
                  update-in [::ana/namespaces target-ns :defs target-name]
                  merge (dissoc src-data :name :doc))
           (swap! env/*compiler*
                  update-in [::ana/namespaces target-ns :defs target-name :meta]
                  merge (dissoc (:meta src-data) :name :doc))))
    ;; (util/debug-prn "  ---post---copy---")
    ;; (util/debug-prn "  !target      :file" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :file]))
    ;; (util/debug-prn "  !target meta :file" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :meta :file]))
    ;; (util/debug-prn "  !target      :line" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :line]))
    ;; (util/debug-prn "  !target meta :line" (get-in @env/*compiler* [::ana/namespaces target-ns :defs target-name :meta :line]))

    )
  nil)

(defmacro fixup-vars
  "We can't alter-meta! in cljs, some metadata needs to be changed in the compiler state"
  [target-ns & import-data]
  ''(util/debug-prn "fixup vars... ")
  (doall
   (for [[src-sym target-name _] import-data]
     (adjust-var-meta! target-ns target-name src-sym)))
  nil)

;; TODO: del test code
(def first-time (atom true))
(defmacro import-fn
  "Given a function in another namespace, defines a function with the
   same name in the current namespace.  Argument lists, doc-strings,
   and original line-numbers are preserved."
  [src-sym target-name target-meta-changes]
  #_(util/debug-prn "import-fn cljs======>" sym)
  (let [vr (or (resolve-sym src-sym) (throw (ex-info (str "Don't recognize " src-sym) {})))
        m (resolved-meta vr)
        new-meta (-> m (merge target-meta-changes) (dissoc :name))]
    (when (:macro m)
      (throw (ex-info (str "Calling import-fn on a macro: " src-sym) {})))
    #_(util/debug-prn "import-fn pre do... vr" (pretty-str vr) )
    #_(util/debug-prn "import-fn pre do... m" (pretty-str m) )
    #_(util/debug-prn "import-fn pre do... n" n )
    ;;(util/debug-prn "import-fn pre do... (var n)" (var n))
    ;; altering before existence I think
    ;;(assoc-in @env/*compiler* [::ana/namespaces (:ns m) :defs n :file] "testing/123.clj")
    #_(when @first-time
        (reset! first-time false)
        (util/debug-prn (pr-str @env/*compiler*)))
    `(do
        (def ~(with-meta target-name new-meta) ~(:name vr)))))

;; TODO: I don't expect our project will make use of this one...
;; TODO: untested
(defmacro import-def
  "Given a regular def'd var from another namespace, defined a new var with the
   same name in the current namespace."
  [src-sym target-name target-meta-changes]
  #_(util/debug-prn "cljs import-def---ddd-->" sym)
  (let [vr (or (resolve-sym src-sym) (throw (ex-info (str "Don't recognize " src-sym) {})))
        m (resolved-meta vr)
        new-meta (-> m (merge target-meta-changes) (dissoc :name))
        ;; TODO: verify dynamic, this is not correct
        ;;n (with-meta n (if (:dynamic m) {:dynamic true} {}))
        ]
    #_(util/debug-prn "import-def pre do...vr\n" (pretty-str vr))
    #_(util/debug-prn "import-def pre do... m\n" (pretty-str m))
    `(do
       (def ~(with-meta target-name new-meta) ~(:name vr)))))



(defmacro import-vars
  "Imports a list of vars from other namespaces."
  [& raw-syms]
  (let [syms (helper/unravel-syms raw-syms)
        import-data (map
                     (fn [[sym opts]]
                       (let [vr (or (resolve-sym sym) (throw (ex-info (str "Don't recognize " sym) {})))
                             m (resolved-meta vr)
                             n (:name m)]
                         [sym (helper/new-name n opts) (helper/new-meta m opts)]))
                     syms)
        import-cmds (map
                     (fn [[sym new-name new-meta]]
                       (let [vr (resolve-sym sym)
                             m (resolved-meta vr)]
                         (cond
                           (:macro m)     (throw (ex-info (str "please handle macro imports in clj: " sym) {}))
                           (:arglists m) `(import-fn ~sym ~new-name ~new-meta)
                           :else         `(import-def ~sym ~new-name ~new-meta))))
                     import-data)
        cmds (concat import-cmds [`(fixup-vars ~*ns* ~@import-data)])]
    `(do ~@cmds)))

;; TODO delete all tests junk
(defmacro erp2[& args]
  (println "erp2" args))

(defmacro erp[& args]
  (let [cmds (map (fn[x] `(println ~x)) args)
        cmds (concat cmds [`(erp2 ~@args)])]
    `(do ~@cmds)))
;; --- potemkin.types

;; TODO: assuming this has no value for cljs??
(defmacro defprotocol+
  "A simpler version of 'potemkin.types/defprotocol+'."
  [name & body]
  `(defprotocol ~name ~@body))


(comment
  (concat (map inc [1 2 3]) [20])

  (erp ["a" "b" "c"]
       ["d" "e"])

  (def a (atom {:namespaces {'clojure.string {:defs {'sym1 {:meta {:file "file" :line 33}}}}}}))
  (select-keys {:a 1 :b 2 :c 3} [:a :c])
  (swap! a update-in [:namespaces 'clojure.string :defs 'sym1 :meta] merge {:file "newfile" :line 44})
  (import-vars [string join split]))
