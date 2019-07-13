#!/usr/bin/env bash

cd ../cljdoc
./script/cljdoc ingest -p lread/rewrite-cljs-playground -v 0.4.5-SNAPSHOT --git ~/other-proj/rewrite-cljs-playground

#./script/cljdoc ingest -p rewrite-cljs \
#  -v 0.4.5-SNAPSHOT \
#  --jar ~/.m2/repository/rewrite-cljs/rewrite-cljs/0.4.5-SNAPSHOT/rewrite-cljs-0.4.5-SNAPSHOT.jar \
#  --pom ~/.m2/repository/rewrite-cljs/rewrite-cljs/0.4.5-SNAPSHOT/rewrite-cljs-0.4.5-SNAPSHOT.pom \
#  --git ~/other-project/rewrite-cljs \
#  --rev "cljc-spike-2"
