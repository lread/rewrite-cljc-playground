(ns rewrite-clj.impl.potemkin-test
  (:require [clojure.test :refer [deftest is are testing run-tests]]
            [rewrite-clj.impl.potemkin-t1]
            [rewrite-clj.impl.potemkin-t2] )
  #?(:clj (:require [rewrite-clj.impl.potemkin.clojure :refer [import-vars]])
     :cljs (:require-macros [rewrite-clj.impl.potemkin.clojurescript :refer [import-vars]])))

#?(:cljs
   (import-vars [rewrite-clj.impl.potemkin-t1 f fd]))

#?(:cljs
   (deftest t-straight-imports-cljs
     (is (= (dissoc (meta #'f) :ns) (dissoc (meta #'rewrite-clj.impl.potemkin-t1/f) :ns)))
     (is (= (:doc (meta #'fd)) "function with doc") )
     (is (= (:line (meta #'fd) 9)))
     (is (= (:file (meta #'fd)) "/Users/lee/other-proj/rewrite-cljs/test/cljc/rewrite_clj/impl/potemkin_t1.cljc"))))

#_(:clj
   (import-vars
    [rewrite-clj.impl.potemkin-t1 d dd f fd m md]
    {:sym-to-pattern "mod-@@orig-name@@"
     :doc-to-pattern "Orig sym: @@orig-name@@, orig doc: @@orig-doc@@"}
    [rewrite-clj.impl.potemkin-t2 d dd f fd m md]))

;; TODO: tests for cljs - macros should probably fail by design but defs and fns should work at least in a limited way
#_(:clj
   (deftest t-straight-imports
     (are [?dest ?src]
         (is (= (meta (var ?dest)) (meta (var ?src))))
       d  rewrite-clj.impl.potemkin-t1/d
       dd rewrite-clj.impl.potemkin-t1/dd
       f  rewrite-clj.impl.potemkin-t1/f
       fd rewrite-clj.impl.potemkin-t1/fd
       m  rewrite-clj.impl.potemkin-t1/m
       md rewrite-clj.impl.potemkin-t1/md)))

#_(:clj
   (deftest t-modified-imports
     (are [?dest ?src]
         (let [orig-meta (meta (var ?src))
               orig-doc (:doc orig-meta)
               orig-name (:name orig-meta)
               new-name (symbol (str "mod-" orig-name))
               new-meta (assoc orig-meta
                               :name new-name
                               :doc (str "Orig sym: " orig-name ", orig doc: " orig-doc))]
           (is (= (meta (var ?dest)) new-meta)))
       mod-d  rewrite-clj.impl.potemkin-t2/d
       mod-dd rewrite-clj.impl.potemkin-t2/d
       mod-f  rewrite-clj.impl.potemkin-t2/f
       mod-fd rewrite-clj.impl.potemkin-t2/fd
       mod-m  rewrite-clj.impl.potemkin-t2/m
       mod-md rewrite-clj.impl.potemkin-t2/md)))
