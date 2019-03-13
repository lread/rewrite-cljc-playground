(ns rewrite-clj.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [rewrite-clj.node-test]
            [rewrite-clj.paredit-test]
            [rewrite-clj.parser-test]
            [rewrite-clj.regression-test]
            [rewrite-clj.zip-test]
            [rewrite-clj.node.coerce-test]
            [rewrite-clj.node.node-test]
            [rewrite-clj.zip.base-test]
            [rewrite-clj.zip.editz-test]
            [rewrite-clj.zip.findz-test]
            [rewrite-clj.zip.insert-test]
            [rewrite-clj.zip.move-test]
            [rewrite-clj.zip.remove-test]
            [rewrite-clj.zip.seqz-test]
            [rewrite-clj.zip.subedit-test]
            [rewrite-clj.zip.walk-test]
            [rewrite-clj.zip.whitespace-test]
            [rewrite-clj.custom-zipper.core-test]
            [rewrite-clj.custom-zipper.utils-test]))

(doo-tests 'rewrite-clj.node-test
           'rewrite-clj.paredit-test
           'rewrite-clj.parser-test
           'rewrite-clj.regression-test
           'rewrite-clj.zip-test
           'rewrite-clj.node.coerce-test
           'rewrite-clj.node.node-test
           'rewrite-clj.zip.base-test
           'rewrite-clj.zip.editz-test
           'rewrite-clj.zip.findz-test
           'rewrite-clj.zip.insert-test
           'rewrite-clj.zip.move-test
           'rewrite-clj.zip.remove-test
           'rewrite-clj.zip.seqz-test
           'rewrite-clj.zip.subedit-test
           'rewrite-clj.zip.walk-test
           'rewrite-clj.zip.whitespace-test
           'rewrite-clj.custom-zipper.core-test
           'rewrite-clj.custom-zipper.utils-test)
