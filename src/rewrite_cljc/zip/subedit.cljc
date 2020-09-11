(ns ^:no-doc rewrite-cljc.zip.subedit
  (:require [rewrite-cljc.custom-zipper.core :as z]
            [rewrite-cljc.zip.base :as base])
  #?(:cljs (:require-macros [rewrite-cljc.zip.subedit])))

;; ## Edit Scope

(defn- path
  "Generate a seq representing a path to the current node
   starting at the root. Each element represents one `z/down`
   and the value of each element will be the number of `z/right`s
   to run."
  [zloc]
  (->> (iterate z/up zloc)
       (take-while z/up)
       (map (comp count z/lefts))
       (reverse)))

(defn- move-step
  "Move one down and `n` steps to the right."
  [loc n]
  (nth
    (iterate z/right (z/down loc))
    n))

(defn- move-to
  "Move to the node represented by the given path."
  [zloc path]
  (let [root (-> zloc z/root base/edn*)]
    (reduce move-step root path)))

(defn edit-node
  "Return zipper applying function `f` to `zloc`. The resulting
   zipper will be located at the same path (i.e. the same number of
   downwards and right movements from the root) incoming `zloc`."
  [zloc f]
  (let [zloc' (f zloc)]
    (assert (not (nil? zloc')) "function applied in 'edit-node' returned nil.")
    (move-to zloc' (path zloc))))

(defmacro edit->
  "Like `->`. Threads `zloc` through forms.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as incoming `zloc`."
  [zloc & body]
  `(edit-node ~zloc #(-> % ~@body)))

(defmacro edit->>
  "Like `->>`. Threads `zloc` through forms.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as incoming `zloc`."
  [zloc & body]
  `(edit-node ~zloc #(->> % ~@body)))

;; ## Sub-Zipper

(defn subzip
  "Create and return a zipper whose root is the current node in `zloc`."
  [zloc]
  (let [zloc' (some-> zloc z/node base/edn*)]
    (assert zloc' "could not create subzipper.")
    zloc'))

(defn subedit-node
  "Return zipper replacing current node in `zloc` with result of `f` applied to said node as an isolated sub-tree.
   The resulting zipper will be located on the root of the modified sub-tree."
  [zloc f]
  (let [zloc' (f (subzip zloc))]
    (assert (not (nil? zloc')) "function applied in 'subedit-node' returned nil.")
    (z/replace zloc (z/root zloc'))))

(defmacro subedit->
  "Like `->`. Threads `zloc`, as an isolated sub-tree through forms, then zips
   up to, and locates at, the root of the modified sub-tree."
  [zloc & body]
  `(subedit-node ~zloc #(-> % ~@body)))

(defmacro subedit->>
  "Like `->`. Threads `zloc`, as an isolated sub-tree through forms, then zips
      up to, and locates at, the root of the modified sub-tree."
  [zloc & body]
  `(subedit-node ~zloc #(->> % ~@body)))
