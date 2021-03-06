(defproject clj-chat "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.494"]
                 [reagent "0.6.0"]
                 [re-frame "0.9.2"]
                 [compojure "1.5.0"]
                 [yogthos/config "0.8"]
                 [ring "1.5.1"]
                 [ring/ring-defaults "0.2.3"]
                 [com.taoensso/sente "1.11.0"]
                 [org.clojure/core.async "0.3.426"]
                 [http-kit "2.2.0"]
                 [com.andrewmcveigh/cljs-time "0.5.0-alpha2"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler clj-chat.handler/dev-handler}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]]

    :plugins      [[lein-figwheel "0.5.9"]]
    }}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "clj-chat.core/mount-root"}
     :compiler     {:main                 clj-chat.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler     {:main            clj-chat.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :output-dir      "resources/public/js/compiled"
                    :asset-path      "js/compiled"
                    :optimizations   :simple
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}


    ]}

  :main clj-chat.server

  :aot [clj-chat.server]

  :uberjar-name "clj-chat.jar"

  :prep-tasks [["cljsbuild" "once" "min"] "compile"]
  )
