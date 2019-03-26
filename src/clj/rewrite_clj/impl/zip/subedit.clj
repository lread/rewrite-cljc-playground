(ns ^:no-doc rewrite-clj.impl.zip.subedit)

(defmacro edit->
  "Like `->`, applying the given function to the current zipper location.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as the original
   node."
  [zloc & body]
  `(edit-node ~zloc #(-> % ~@body)))

(defmacro edit->>
  "Like `->>`, applying the given function to the current zipper location.
   The resulting zipper will be located at the same path (i.e. the same
   number of downwards and right movements from the root) as the original
   node."
  [zloc & body]
  `(edit-node ~zloc #(->> % ~@body)))

(defmacro subedit->
  "Like `->`, applying modifications to the current sub-tree, zipping
   up to the current location afterwards."
  [zloc & body]
  `(subedit-node ~zloc #(-> % ~@body)))

(defmacro subedit->>
  "Like `->>`, applying modifications to the current sub-tree, zipping
   up to the current location afterwards."
  [zloc & body]
  `(subedit-node ~zloc #(->> % ~@body)))
