(defproject mixer "0.1.0-SNAPSHOT"
  :description "A simple music mixer"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.339"]]

  :source-paths ["src"]

  :aliases {"fig"       ["trampoline" "run" "-m" "figwheel.main"]
            "fig:build" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]
            "fig:min"   ["run" "-m" "figwheel.main" "-O" "advanced" "-bo" "dev"]}


  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.1.9"]
                                  [cider/piggieback "0.3.10"]
                                  [org.omcljs/om "1.0.0-beta1"]
                                  [cljs-ajax "0.7.3"]
                                  [sablono "0.8.4"]]}
             :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}
             }
  )
