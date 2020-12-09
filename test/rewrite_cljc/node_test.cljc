(ns rewrite-cljc.node-test
  "This test namespace originated from rewrite-cljs."
  (:require [clojure.test :refer [deftest is testing]]
            [rewrite-cljc.node :as n]
            [rewrite-cljc.parser :as p]))

(deftest namespaced-keyword
  (is (= ":dill/dall"
         (n/string (n/keyword-node :dill/dall)))))

(deftest funky-keywords
  (is (= ":%dummy.*"
         (n/string (n/keyword-node :%dummy.*)))))

(deftest regex-node
  (let [sample "(re-find #\"(?i)RUN\" s)"
        sample2 "(re-find #\"(?m)^rss\\s+(\\d+)$\")"
        sample3 "(->> (str/split container-name #\"/\"))"]
    (is (= sample (-> sample p/parse-string n/string)))
    (is (= sample2 (-> sample2 p/parse-string n/string)))
    (is (= sample3 (-> sample3 p/parse-string n/string)))))


(deftest regex-with-newlines
  (let [sample "(re-find #\"Hello
        \\nJalla\")"]
    (is (= sample (-> sample p/parse-string n/string)))))



(deftest reader-conditionals
  (testing "Simple reader conditional"
    (let [sample "#?(:clj bar)"
          res (p/parse-string sample)]
      (is (= sample (n/string res)))
      (is (= :reader-macro (n/tag res)))
      (is (= [:token :list] (map n/tag (n/children res))))))

  (testing "Reader conditional with space before list"
    (let [sample "#? (:clj bar)"
          sample2 "#?@ (:clj bar)"]
      (is (= sample (-> sample p/parse-string n/string)))
      (is (= sample2 (-> sample2 p/parse-string n/string)))))


  (testing "Reader conditional with splice"
    (let [sample
"(:require [clojure.string :as s]
           #?@(:clj  [[clj-time.format :as tf]
                      [clj-time.coerce :as tc]]
               :cljs [[cljs-time.coerce :as tc]
                      [cljs-time.format :as tf]]))"
          res (p/parse-string sample)]
      (is (= sample (n/string res))))))

(deftest t-node?
  (is (not (n/node? 42)))
  (is (not (n/node? "just a string")))
  (is (not (n/node? {:a 1})))
  (is (not (n/node? (first {:a 1}))))
  (is (n/node? (n/list-node (list 1 2 3))))
  (is (n/node? (n/string-node "123"))))

(deftest t-keyword-node-sexpr
  (let [opts {:auto-resolve (fn [alias]
                              (if (= :current alias)
                                'my.current.ns
                                (get {'my-alias 'my.aliased.ns
                                      'nsmap-alias 'nsmap.aliased.ns}
                                     alias
                                     (symbol (str alias "-unresolved")))))}]
    (testing "keyword with default resolver"
      (is (= :my-kw (-> (n/keyword-node :my-kw) n/sexpr)))
      (is (= :_/my-kw (-> (n/keyword-node :_/my-kw) n/sexpr)))
      (is (= :my-prefix/my-kw (-> (n/keyword-node :my-prefix/my-kw) n/sexpr)))
      (is (= :user/my-kw (-> (n/keyword-node :my-kw true) n/sexpr)))
      (is (= :my-alias-unresolved/my-kw (-> (n/keyword-node :my-alias/my-kw true) n/sexpr))))
    (testing "keyword with custom resolver"
      (is (= :my-kw (-> (n/keyword-node :my-kw) (n/sexpr opts))))
      (is (= :my-prefix/my-kw (-> (n/keyword-node :my-prefix/my-kw) (n/sexpr opts))))
      (is (= :my.current.ns/my-kw (-> (n/keyword-node :my-kw true) (n/sexpr opts))))
      (is (= :my.aliased.ns/my-kw (-> (n/keyword-node :my-alias/my-kw true) (n/sexpr opts)))))
    (testing "map qualified keyword with default resolver"
      (is (= :my-kw
             (-> (n/keyword-node :_/my-kw)
                 (assoc :map-qualifier {:auto-resolved? false :prefix "nsmap-prefix"})
                 n/sexpr)))
      (is (= :nsmap-prefix/my-kw
             (-> (n/keyword-node :my-kw)
                 (assoc :map-qualifier {:auto-resolved? false :prefix "nsmap-prefix"})
                 n/sexpr)))
      (is (= :user/my-kw
             (-> (n/keyword-node :my-kw)
                 (assoc :map-qualifier {:auto-resolved? true})
                 n/sexpr)))
      (is (= :nsmap-alias-unresolved/my-kw
             (-> (n/keyword-node :my-kw)
                 (assoc :map-qualifier {:auto-resolved? true :prefix "nsmap-alias"})
                 n/sexpr)))
      (is (= :kw-prefix/my-kw
             (-> (n/keyword-node :kw-prefix/my-kw)
                 (assoc :map-qualifier {:auto-resolved? false :prefix "nsmap-prefix"})
                 n/sexpr))))
    (testing "map qualified keyword with custom rsolver"
      (is (= :my-kw
             (-> (n/keyword-node :_/my-kw)
                 (assoc :map-qualifier {:auto-resolved? false :prefix "nsmap-prefix"})
                 (n/sexpr opts))))
      (is (= :nsmap-prefix/my-kw
             (-> (n/keyword-node :my-kw)
                 (assoc :map-qualifier {:auto-resolved? false :prefix "nsmap-prefix"})
                 (n/sexpr opts))))
      (is (= :my.current.ns/my-kw
             (-> (n/keyword-node :my-kw)
                 (assoc :map-qualifier {:auto-resolved? true})
                 (n/sexpr opts))))
      (is (= :nsmap.aliased.ns/my-kw
             (-> (n/keyword-node :my-kw)
                 (assoc :map-qualifier {:auto-resolved? true :prefix "nsmap-alias"})
                 (n/sexpr opts))))
      (is (= :kw-prefix/my-kw
             (-> (n/keyword-node :kw-prefix/my-kw)
                 (assoc :map-qualifier {:auto-resolved? false :prefix "nsmap-prefix"})
                 (n/sexpr opts)))))
    (testing "when auto-resolver returns nil, bare or already qualified kw is returned"
      (let [opts {:auto-resolve (fn [_alias])}]
        (is (= :my-kw (-> (n/keyword-node :my-kw true) (n/sexpr opts))))
        (is (= :my-kw (-> (n/keyword-node :my-alias/my-kw true) (n/sexpr opts))))
        (is (= :my-kw
               (-> (n/keyword-node :foo/my-kw true)
                   (assoc :map-qualifier {:auto-resolved? true :prefix "nsmap-alias"})
                   (n/sexpr opts))))
        (is (= :foo/my-kw
               (-> (n/keyword-node :foo/my-kw false)
                   (assoc :map-qualifier {:auto-resolved? true :prefix "nsmap-alias"})
                   (n/sexpr opts)))) ))))
