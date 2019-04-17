(ns rewrite-clj.potemkin-test
  (:require [clojure.test :refer [deftest is are testing run-tests]]
            [rewrite-clj.potemkin-t1 :include-macros true]
            [rewrite-clj.potemkin-t2 :include-macros true] )
  #?(:clj (:require [rewrite-clj.potemkin.clojure :refer [import-vars]])
     :cljs (:require-macros [rewrite-clj.potemkin.cljs :refer [import-vars]]
                            ;; macros need to be required for cljs
                            [rewrite-clj.potemkin-test :refer [t-macro t-macro-doc mod-t-macro mod-t-macro-doc]])))

;; import macros in clj context to make them available for both clj and cljs
#?(:clj
   (import-vars
    [rewrite-clj.potemkin-t1 t-macro t-macro-doc]
    {:sym-to-pattern "mod-@@orig-name@@"
     :doc-to-pattern "Orig sym: @@orig-name@@, orig doc: @@orig-doc@@"}
    [rewrite-clj.potemkin-t2 t-macro t-macro-doc]))

;; import rest non-macros both contexts
(import-vars
 [rewrite-clj.potemkin-t1 t-def t-def-doc t-fn t-fn-doc]
 {:sym-to-pattern "mod-@@orig-name@@"
  :doc-to-pattern "Orig sym: @@orig-name@@, orig doc: @@orig-doc@@"}
 [rewrite-clj.potemkin-t2 t-def t-def-doc t-fn t-fn-doc])

(defn- get-meta
  "The ns is not copied over for cljs. I *think* that is ok and probably good? Perhaps I should dupe behaviour for clj."
  [test-sym]
  (let [md #?(:cljs (dissoc (meta test-sym) :ns)
              :clj (meta test-sym))]
    (is (not (empty? md)) "no metadata, test malconfigured?")
    md))

(deftest t-straight-imports-meta-data
  (are [?dest ?src]
      (is (= (get-meta ?src) (get-meta ?dest)))
    #'t-fn      #'rewrite-clj.potemkin-t1/t-fn
    #'t-fn-doc  #'rewrite-clj.potemkin-t1/t-fn-doc
    #'t-def     #'rewrite-clj.potemkin-t1/t-def
    #'t-def-doc #'rewrite-clj.potemkin-t1/t-def-doc))

#?(:clj
   (deftest t-straight-imports-macros-meta-data
     (are [?dest ?src]
         (is (= (get-meta ?src) (get-meta ?dest)))
       #'t-macro     #'rewrite-clj.potemkin-t1/t-macro
       #'t-macro-doc #'rewrite-clj.potemkin-t1/t-macro-doc)))

(defn- expected-modified-meta[src]
  (let [src-meta (get-meta src)
        src-doc (:doc src-meta)
        src-name (:name src-meta)
        expected-name (symbol (str "mod-" src-name))]
    (assoc src-meta
           :name expected-name
           :doc (str "Orig sym: " src-name ", orig doc: " src-doc))))

(deftest t-modified-imports-meta-data
  (are [?dest ?src]
      (is (= (expected-modified-meta ?src) (get-meta ?dest)))
    #'mod-t-fn      #'rewrite-clj.potemkin-t2/t-fn
    #'mod-t-fn-doc  #'rewrite-clj.potemkin-t2/t-fn-doc
    #'mod-t-def     #'rewrite-clj.potemkin-t2/t-def
    #'mod-t-def-doc #'rewrite-clj.potemkin-t2/t-def-doc))

#?(:clj
   (deftest t-modified-imports-macros-meta-data
     (are [?dest ?src]
         (is (= (expected-modified-meta ?src) (get-meta ?dest)))
       #'mod-t-macro     #'rewrite-clj.potemkin-t2/t-macro
       #'mod-t-macro-doc #'rewrite-clj.potemkin-t2/t-macro-doc)))

(deftest t-imports-evaluation-equivalent
  (is (= 42      t-def                     rewrite-clj.potemkin-t1/t-def))
  (is (= 77      t-def-doc                 rewrite-clj.potemkin-t1/t-def-doc))
  (is (= 33      (t-fn 33)                 (rewrite-clj.potemkin-t1/t-fn 33)))
  (is (= 27      (t-fn-doc 27)             (rewrite-clj.potemkin-t1/t-fn-doc 27)))
  (is (= "ok"    (t-macro "ok")            (rewrite-clj.potemkin-t1/t-macro "ok")))
  (is (= "1234"  (t-macro-doc 1 2 3 4)     (rewrite-clj.potemkin-t1/t-macro-doc 1 2 3 4)))
  (is (= 242     mod-t-def                 rewrite-clj.potemkin-t2/t-def))
  (is (= 277     mod-t-def-doc             rewrite-clj.potemkin-t2/t-def-doc 277))
  (is (= 233     (mod-t-fn 33)             (rewrite-clj.potemkin-t2/t-fn 33)))
  (is (= 227     (mod-t-fn-doc 27)         (rewrite-clj.potemkin-t2/t-fn-doc 27)))
  (is (= "2ok"   (mod-t-macro "ok")        (rewrite-clj.potemkin-t2/t-macro "ok")))
  (is (= "21234" (mod-t-macro-doc 1 2 3 4) (rewrite-clj.potemkin-t2/t-macro-doc 1 2 3 4))))
