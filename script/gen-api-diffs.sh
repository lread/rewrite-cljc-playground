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

status-line() {
    script/status-line "$1" "$2"
}

# TODO: maybe this should change after dev is over
# TODO: but for now working on local copy of rewrite-cljc
status-line info "installing rewrite-cljc locally"
mvn install
DEL_CACHE_SPEC=".diff-apis/.cache/$(echo "${REWRITE_CLJC}" | tr '/' '-')*"
# shellcheck disable=SC2086
rm -rf $DEL_CACHE_SPEC

status-line info "Diffing rewrite-clj and rewrite-cljs"
clojure -Adiff-apis \
        rewrite-clj 0.6.1 clj \
        rewrite-cljs 0.4.4 cljs \
        --arglists-by :arity-only \
        --report-format :asciidoc \
        --notes ${NOTES_DIR}/rewrite-clj-and-rewrite-cljs.adoc \
        --report-filename "${REPORT_DIR}/rewrite-clj-and-rewrite-cljs.adoc"

status-line info "Diffing rewrite-clj and rewrite-cljc"
clojure -Adiff-apis \
        rewrite-clj 0.6.1 clj \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} clj \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-cljc \
        --exclude-namespace rewrite-clj.potemkin \
        --exclude-namespace rewrite-cljc.potemkin \
        --exclude-namespace rewrite-cljc.potemkin.cljs \
        --exclude-namespace rewrite-cljc.potemkin.clojure \
        --exclude-namespace rewrite-cljc.potemkin.helper \
        --exclude-namespace rewrite-cljc.custom-zipper.switchable \
        --exclude-namespace rewrite-cljc.interop \
        --notes ${NOTES_DIR}/rewrite-clj-and-rewrite-cljc-clj.adoc \
        --replace-b-namespace '^rewrite-cljc/rewrite-clj' \
        --report-format :asciidoc \
        --report-filename "${REPORT_DIR}/rewrite-clj-and-rewrite-cljc-clj.adoc"

status-line info "Diffing rewrite-cljs and rewrite-cljc"
clojure -Adiff-apis \
        rewrite-cljs 0.4.4 cljs \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} cljs \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-cljc \
        --exclude-namespace rewrite-clj.potemkin \
        --exclude-namespace rewrite-cljc.potemkin \
        --exclude-namespace rewrite-cljc.potemkin.cljs \
        --exclude-namespace rewrite-cljc.potemkin.clojure \
        --exclude-namespace rewrite-cljc.potemkin.helper \
        --exclude-namespace rewrite-cljc.custom-zipper.switchable \
        --exclude-namespace rewrite-cljc.interop \
        --notes ${NOTES_DIR}/rewrite-cljs-and-rewrite-cljc-cljs.adoc \
        --replace-b-namespace '^rewrite-cljc/rewrite-clj' \
        --report-format :asciidoc \
        --report-filename "${REPORT_DIR}/rewrite-cljs-and-rewrite-cljc-cljs.adoc"

status-line info "Diffing rewrite-cljc cljs vs clj"
clojure -Adiff-apis \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} cljs \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} clj \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-cljc.potemkin.clojure \
        --notes ${NOTES_DIR}/rewrite-cljc.adoc \
        --report-format :asciidoc \
        --report-filename "${REPORT_DIR}/rewrite-cljc.adoc"

status-line info "Diffing rewrite-cljc cljs vs clj - documented apis only"
clojure -Adiff-apis \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} cljs \
        ${REWRITE_CLJC} ${REWRITE_CLJC_VERSION} clj \
        --arglists-by :arity-only \
        --exclude-namespace rewrite-cljc.potemkin.clojure \
        --exclude-with :no-doc \
        --exclude-with :skip-wiki \
        --notes ${NOTES_DIR}/rewrite-cljc-documented-only.adoc \
        --report-format :asciidoc \
        --report-filename "${REPORT_DIR}/rewrite-cljc-documented-only.adoc"
