(ns rewrite-clj.impl.potemkin-test
  (:require [clojure.test :refer [deftest is are testing run-tests]]
            [rewrite-clj.impl.potemkin-t1]
            [rewrite-clj.impl.potemkin-t2] )
  #?(:clj (:require [rewrite-clj.impl.potemkin.clojure :refer [import-vars]])
     :cljs (:require-macros [rewrite-clj.impl.potemkin.clojurescript :refer [import-vars]])))

(import-vars
 [rewrite-clj.impl.potemkin-t1 d dd f fd]
 {:sym-to-pattern "mod-@@orig-name@@"
  :doc-to-pattern "Orig sym: @@orig-name@@, orig doc: @@orig-doc@@"}
 [rewrite-clj.impl.potemkin-t2 d dd f fd])

(defn get-meta
  "The ns is not copied over for cljs. I *think* that is ok and probably good?"
  [sym]
  (let [md #?(:cljs (dissoc (meta sym) :ns)
              :clj (meta sym))]
    (is (not (empty? md)) "no metadata, test malconfigured?")
    md))

(deftest t-straight-imports-meta-data
  (are [?dest ?src]
      (is (= (get-meta ?src) (get-meta ?dest)))
    #'f   #'rewrite-clj.impl.potemkin-t1/f
    #'fd  #'rewrite-clj.impl.potemkin-t1/fd
    #'d   #'rewrite-clj.impl.potemkin-t1/d
    #'dd  #'rewrite-clj.impl.potemkin-t1/dd))

(deftest t-modified-imports-meta-data
  (are [?dest ?src]
      (let [src-meta (get-meta ?src)
            src-doc (:doc src-meta)
            src-name (:name src-meta)
            expected-name (symbol (str "mod-" src-name))
            expected-meta (assoc src-meta
                            :name expected-name
                            :doc (str "Orig sym: " src-name ", orig doc: " src-doc))]

        (is (= expected-meta (get-meta ?dest))))
    #'mod-f   #'rewrite-clj.impl.potemkin-t2/f
    #'mod-fd  #'rewrite-clj.impl.potemkin-t2/fd
    #'mod-d   #'rewrite-clj.impl.potemkin-t2/d
    #'mod-dd  #'rewrite-clj.impl.potemkin-t2/dd))

;; TODO:  macros - can we import for cljs... and if we can does that conflict with a clj import?
