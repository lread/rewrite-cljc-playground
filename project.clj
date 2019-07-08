(defproject rewrite-cljs "0.4.5-SNAPSHOT"
  :description "Comment-/Whitespace-preserving rewriting of EDN documents."
  :url "https://github.com/clj-commons/rewrite-cljs"
  :scm {;;TODO: point to real repo
        :name "git"
        :url "https://github.com/lread/rewrite-cljs-playground.git"
        :tag "cc148d3cae40bb3ef67aaa8ab86b76e684283057"}
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}

  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :source-paths ["src"]

  :clean-targets ^{:protect false} [:target-path :compile-path "out"]

  :doo {:build "test"
        :karma {:config {"plugins" ["karma-junit-reporter"]
                         "reporters" ["progress" "junit"]
                         "junitReporter" {"outputDir" "target/out/test-results/cljs"}}}}

  :profiles {:1.9 {:lein-tools-deps/config {:resolve-aliases [:1.9]}}
             :kaocha {:lein-tools-deps/config {:resolve-aliases [:kaocha]}}
             :fig-test {:lein-tools-deps/config {:resolve-aliases [:fig-test]}
                        :resource-paths ["target"]}
             :doo-test {:plugins [[lein-doo "0.1.11"]]}
             :dev {:lein-tools-deps/config {:resolve-aliases [:dev]}
                   :exclusions [org.clojure/clojure]
                   :plugins [[lein-cljsbuild "1.1.7"]]
                   :cljsbuild {:builds
                               [{:id "fail-on-warning"
                                 :source-paths ["test"]
                                 :warning-handlers [rewrite-clj.warning-handler/suppressor]
                                 :compiler {:output-dir "target/cljsbuild/fail-test/out"
                                            :output-to "target/cljsbuild/fail-test/test.js"
                                            :main rewrite-clj.doo-test-runner
                                            :source-map true
                                            :optimizations :none
                                            :pretty-print true}}
                                {:id "test"
                                 :source-paths ["test"]
                                 :compiler {:output-dir "target/cljsbuild/test/out"
                                            :output-to "target/cljsbuild/test/test.js"
                                            :main rewrite-clj.doo-test-runner
                                            :source-map true
                                            :optimizations :none
                                            :warnings {:fn-deprecated false}
                                            :pretty-print true}}
                                {:id "node-test"
                                 :source-paths ["test"]
                                 :compiler {:output-dir "target/cljsbuild/node-test/out"
                                            :output-to "target/cljsbuild/test/node-test.js"
                                            :main rewrite-clj.doo-test-runner
                                            :target :nodejs
                                            :optimizations :none
                                            :warnings {:fn-deprecated false}
                                            :pretty-print true}}]}}}

  :aliases {"kaocha"
            ^{:doc "base kaocha - use to run all clj tests once for default clojure env"}
            ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]

            "_kaocha-junit"
            ^{:doc "internal base to configure for kaocha junit"}
            ["kaocha" "--reporter" "documentation" "--plugin" "kaocha.plugin/junit-xml"]

            "clj-junit-1.9"
            ^{:doc "run all clj tests under clojure 1.9 - output to junit. For ci server."}
            ["with-profile" "dev,1.9" "_kaocha-junit" "--junit-xml-file" "target/out/test-results/clj-v1.9/results.xml"]

            "clj-junit-1.10"
            ^{:doc "run all clj tests under clojure 1.10 - output to junit. For ci server."}
            ["with-profile" "dev" "_kaocha-junit" "--junit-xml-file" "target/out/test-results/clj-v1.10/results.xml"]

            "_clj-all"
            ^{:doc "internal base to select all clojure versions"}
            ["with-profile" "dev,1.9:dev"]

            "clj-test-envs-junit"
            ^{:doc "run all clj tests under all supported versions of clojure -output to junit. For ci server."}
            ["do" "clj-junit-1.9," "clj-junit-1.10"]

            "clj-test-envs"
            ^{:doc "run all clj tests under all supported versions of clojure - for dev sanity test."}
            ["_clj-all" "kaocha"]

            "_cljs-chrome-headless"
            ^{:doc "internal base to setup for chrome headless"}
            ["with-profile" "doo-test,dev" "doo" "chrome-headless"]

            "cljs-fail-on-warning"
            ^{:doc "compile cljs source and fail on any warning - ignoring fn-deprecations we are ok with"}
            ["cljsbuild" "once" "fail-on-warning"]

            "_cljs-node"
            ^{:doc "internal base to setup for nodejs"}
            ["with-profile" "doo-test,dev" "doo" "node"]

            "cljs-test-chrome-headless"
            ^{:doc "run all cljs tests under chrome headless"}
            ["_cljs-chrome-headless" "test" "once"]

            "cljs-test-node"
            ^{:doc "run all cljs tests under nodejs"}
            ["_cljs-node" "node-test" "once"]

            "cljs-test-envs"
            ^{:doc "run all cljs tests for all supported environments"}
            ["do" "cljs-fail-on-warning," "cljs-test-chrome-headless," "cljs-test-node"]

            "test-all"
            ^{:doc "run all clj and cljs tests for all supported environments"}
            ["do" "clean," "clj-test-envs-junit," "cljs-test-envs"]

            "clj-auto-test"
            ^{:doc "run all clj tests, watch and rerun automatically"}
            ["kaocha" "--watch"]

            "chrome-auto-test"
            ^{:doc "run all cljs tests under chrome, watch and rerun automatically"}
            ["with-profile" "dev,doo-test" "doo" "chrome" "test" "auto"]

            "fig-test"
            ^{:doc "run figwheel main, find your tests at http://localhost:9500/figwheel-extra-main/auto-testing"}
            ["trampoline" "with-profile" "dev,fig-test" "run" "-m" "figwheel.main" "--" "-b" "fig" "-r"]})
