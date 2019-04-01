(defproject rewrite-cljs "0.4.5-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/clj-commons/rewrite-cljs"
  :scm {:name "git" :url "https://github.com/clj-commons/rewrite-cljs"}
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}

  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [org.clojure/clojurescript "1.10.520"]
                 [org.clojure/tools.reader "1.3.2"]]
  :source-paths ["src/clj/" "src/cljc/" "src/cljs/"]

  :clean-targets ^{:protect false} [:target-path :compile-path "out"]

  :doo {:build "test"
        :karma {:config {"plugins" ["karma-junit-reporter"]
                         "reporters" ["progress" "junit"]
                         "junitReporter" {"outputDir" "target/out/test-results"}}}}

  :profiles {:1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]
                   :eftest {:report-to-file "target/out/test-results/clj-v1.8-junit.xml"}}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]
                   :eftest {:report-to-file "target/out/test-results/clj-v1.9-junit.xml"}}
             :fig-test {:dependencies [[com.bhauman/figwheel-main "0.2.1-SNAPSHOT"]]
                        :resource-paths ["target"]}
             :doo-test {:dependencies [[doo "0.1.11"]]
                        :plugins [[lein-doo "0.1.11"]]}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]]
                   :exclusions [org.clojure/clojure]
                   :plugins [[lein-cljsbuild "1.1.7"]
                             [lein-eftest "0.5.7"]]
                   :source-paths ["test/cljs/" "test/cljc/"]
                   :eftest {:multithread? false
                            :report eftest.report.junit/report
                            :report-to-file "target/out/test-results/clj-v1.10-junit.xml"}
                   :cljsbuild {:builds
                               [{:id "test"
                                 :source-paths ["test/cljs/" "test/cljc/"]
                                 :compiler {:output-dir "target/cljsbuild/test/out"
                                            :output-to "target/cljsbuild/test/test.js"
                                            :main rewrite-clj.doo-test-runner
                                            :source-map true
                                            :optimizations :none
                                            :warnings {:fn-deprecated false}
                                            :pretty-print true}}
                                {:id "node-test"
                                 :source-paths ["test/cljs/" "test/cljc/"]
                                 :compiler {:output-dir "target/cljsbuild/node-test/out"
                                            :output-to "target/cljsbuild/test/node-test.js"
                                            :main rewrite-clj.doo-test-runner
                                            :target :nodejs
                                            :optimizations :none
                                            :warnings {:fn-deprecated false}}}
                                {:id "prod"
                                 :compiler {:output-dir "target/cjlsbuild/prod/out"
                                            :output-to "target/cljsbuild/prod/prod.js"
                                            :optimizations :advanced}}]}}}

  :aliases {"all-clj" ["with-profile" "dev,1.8:dev,1.9:dev"]
            "test-all-clj" ["all-clj" "test"]
            "test-all-clj-junit" ["all-clj" "eftest"]

            "chrome-headless" ["with-profile" "doo-test,dev" "doo" "chrome-headless"]
            "node" ["with-profile" "doo-test,dev" "doo" "node"]

            "chrome-headless-test" ["chrome-headless" "test" "once"]
            "node-test" ["node" "node-test" "once"]
            "test-all-cljs" ["do" "chrome-headless-test," "node-test"]

            "test-all" ["do" "clean," "test-all-clj-junit," "test-all-cljs"]

            "chrome-auto-test" ["with-profile" "dev,doo-test" "doo" "chrome" "test" "auto"]
            "fig-test" ["trampoline" "with-profile" "dev,fig-test" "run" "-m" "figwheel.main" "--" "-b" "fig" "-r"]})
