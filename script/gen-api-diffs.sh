#!/usr/bin/env bash

# This script is intended to be run as needed.
# It generates asciidoc api diff reports.
# If you change the name of the reports, search for links in other docs.

set -eou pipefail

REPORT_DIR=doc/generated/api-diffs
rm -rf ${REPORT_DIR}
mkdir -p ${REPORT_DIR}

NOTES_DIR=doc/diff-notes

REWRITE_CLJC=lread/rewrite-cljs-playground
REWRITE_CLJC_VERSION=1.0.0-alpha

echo "Diffing rewrite-clj and rewrite-cljs"
clojure -Adiff-apis \
        rewrite-clj 0.6.1 clj \
        rewrite-cljs 0.4.4 cljs \
        --arglists-by :arity-only \
        --report-format :asciidoc \
        --notes ${NOTES_DIR}/rewrite-clj-and-rewrite-cljs.adoc \
        > ${REPORT_DIR}/rewrite-clj-and-rewrite-cljs.adoc

echo "Diffing rewrite-clj and rewrite-cljc"
clojure -Adiff-apis \
        rewrite-clj 0.6.1 clj \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} clj \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-clj \
        --exclude-namespace rewrite-clj.potemkin \
        --exclude-namespace rewrite-clj.potemkin.cls \
        --exclude-namespace rewrite-clj.potemkin.clojure \
        --exclude-namespace rewrite-clj.potemkin.helper \
        --exclude-namespace rewrite-clj.custom-zipper.switchable \
        --exclude-namespace rewrite-clj.interop \
        --report-format :asciidoc \
        --notes ${NOTES_DIR}/rewrite-clj-and-rewrite-cljc-clj.adoc \
        > ${REPORT_DIR}/rewrite-clj-and-rewrite-cljc-clj.adoc

echo "Diffing rewrite-cljs and rewrite-cljc"
clojure -Adiff-apis \
        rewrite-cljs 0.4.4 cljs \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} cljs \
        --arglists-by :arity-only \
        --report-format :asciidoc \
        --notes ${NOTES_DIR}/rewrite-cljs-and-rewrite-cljc-cljs.adoc \
        > ${REPORT_DIR}/rewrite-cljs-and-rewrite-cljc-cljs.adoc

echo "Diffing rewrite-cljc cljs vs clj"
clojure -Adiff-apis \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} cljs \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} clj \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-clj.potemkin.clojure \
        --report-format :asciidoc \
        --notes ${NOTES_DIR}/rewrite-cljc.adoc \
        > ${REPORT_DIR}/rewrite-cljc.adoc

echo "Diffing rewrite-cljc cljs vs clj - documented apis only"
clojure -Adiff-apis \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} cljs \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} clj \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-clj.potemkin.clojure \
        --exclude-with :no-doc \
        --exclude-with :skip-wiki \
        --report-format :asciidoc \
        --notes ${NOTES_DIR}/rewrite-cljc-documented-only.adoc \
        > ${REPORT_DIR}/rewrite-cljc-documented-only.adoc
