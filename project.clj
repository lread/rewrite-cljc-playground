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
                         "junitReporter" {"outputDir" "target/out/test-results/cljs"}}}}

  :profiles {:1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :kaocha {:dependencies [[lambdaisland/kaocha "0.0-418"]
                                     [lambdaisland/kaocha-junit-xml "0.0-70"]]}
             :fig-test {:dependencies [[com.bhauman/figwheel-main "0.2.1-SNAPSHOT"]]
                        :resource-paths ["target"]}
             :doo-test {:plugins [[lein-doo "0.1.11"]]}
             :dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [doo "0.1.11"]]
                   :exclusions [org.clojure/clojure]
                   :plugins [[lein-cljsbuild "1.1.7"]]
                   :source-paths ["test/clj" "test/cljs/" "test/cljc/"]
                   :cljsbuild {:builds
                               [{:id "test"
                                 :source-paths ["test/cljs/" "test/cljc/"]
                                 :warning-handlers [rewrite-clj.warning-handler/suppressor]
                                 :compiler {:output-dir "target/cljsbuild/test/out"
                                            :output-to "target/cljsbuild/test/test.js"
                                            :main rewrite-clj.doo-test-runner
                                            :source-map true
                                            :optimizations :none
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

  :aliases {"kaocha"
            ^{:doc "base kaocha - use to run all clj tests once"}
            ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]

            "kaocha-junit"
            ^{:doc "internal base to configure for kaocha junit"}
            ["kaocha" "--reporter" "documentation" "--plugin" "kaocha.plugin/junit-xml"]

            "clj-junit-1.9"
            ^{:doc "run all clj tests under clojure 1.9 - output to junit. For ci server."}
            ["with-profile" "dev,1.9" "kaocha-junit" "--junit-xml-file" "target/out/test-results/clj-v1.9/results.xml"]

            "clj-junit-1.10"
            ^{:doc "run all clj tests under clojure 1.10 - output to junit. For ci server."}
            ["with-profile" "dev" "kaocha-junit" "--junit-xml-file" "target/out/test-results/clj-v1.10/results.xml"]

            "clj-all"
            ^{:doc "internal base to select all clojure versions"}
            ["with-profile" "dev,1.9:dev"]

            "clj-test-envs-junit"
            ^{:doc "run all clj tests under all supported versions of clojure -output to junit. For ci server."}
            ["do" "clj-junit-1.9," "clj-junit-1.10"]

            "clj-test-envs"
            ^{:doc "run all clj tests under all supported versions of clojure - for dev sanity test."}
            ["clj-all" "kaocha"]

            "cljs-chrome-headless"
            ^{:doc "internal base to setup for chrome headless"}
            ["with-profile" "doo-test,dev" "doo" "chrome-headless"]

            "cljs-node"
            ^{:doc "internal base to setup for nodejs"}
            ["with-profile" "doo-test,dev" "doo" "node"]

            "cljs-test-chrome-headless"
            ^{:doc "run all cljs tests under chrome headless"}
            ["cljs-chrome-headless" "test" "once"]

            "cljs-test-node"
            ^{:doc "run all cljs tests under nodejs"}
            ["cljs-node" "node-test" "once"]

            "cljs-test-envs"
            ^{:doc "run all cljs tests for all supported environments"}
            ["do" "cljs-test-chrome-headless," "cljs-test-node"]

            "test-all"
            ^{:doc "run all clj and cljs tests for all supported environments"}
            ["do" "clean," "clj-test-envs-junit," "cljs-test-envs"]

            "clj-auto-test"
            ^{:doc "run all clj tests, watch and rerun automatically"}
            ["with-profile" "kaocha" "--watch"]

            "chrome-auto-test"
            ^{:doc "run all cljs tests under chrome, watch and rerun automatically"}
            ["with-profile" "dev,doo-test" "doo" "chrome" "test" "auto"]

            "fig-test"
            ^{:doc "run figwheel main, find your tests at http://localhost:9500/figwheel-extra-main/auto-testing"}
            ["trampoline" "with-profile" "dev,fig-test" "run" "-m" "figwheel.main" "--" "-b" "fig" "-r"]})
