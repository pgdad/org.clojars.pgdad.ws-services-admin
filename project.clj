(defproject org.clojars.pgdad.ws-services-admin "1.0.0"
  :description "Admin client for services clj-zoo using websockets"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [aleph "0.3.0-alpha1"]
                 [ring "1.1.0"]
                 [hiccup "1.0.0"]
                 [org.clojure/clojurescript "0.0-1006"]
                 [clj-zoo-watcher "1.0.7"]
                 [zookeeper-clj "0.9.2"]]
  :source-path "src/clj"
  :extra-classpath-dirs ["src/cljs"]
  :plugins [[lein-cljsbuild "0.1.5"]]
  :cljsbuild {:builds
              [{:source-path "src/cljs"
                :compiler {:output-to "resources/public/js/app.js"
                           :optimizations :simple :pretty-print true}}]}
  :ring {:handler .core/app}
  :main board.core)

