(defproject org.clojars.pgdad.ws-services-admin "1.0.0"
  :description "Admin client for services clj-zoo using websockets"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [aleph "0.3.0-alpha1"]
                 [ring "1.1.0"]
                 [hiccup "1.0.0"]
                 [org.clojure/clojurescript "0.0-1011"]
                 [clj-zoo "1.0.8"]
                 [clj-zoo-watcher "1.0.9"]
                 [zookeeper-clj "0.9.2"]]
  :aot-compile [org.clojars.pgdad.ws-services-admin.createpassive
                org.clojars.pgdad.ws-services-admin.core]  :source-path "src/clj"
  :extra-classpath-dirs ["src/cljs"]
  :plugins [[lein-cljsbuild "0.1.8"]]
  :cljsbuild {:builds
              [{:source-path "src/cljs/org/clojars/pgdad/ws_services_admin/load"
                :compiler {:output-to "resources/public/js/load.js"
                           :optimizations :simple :pretty-print true}
                :jar true}
               {:source-path "src/cljs/org/clojars/pgdad/ws_services_admin/active"
                :compiler {:output-to "resources/public/js/active.js"
                           :optimizations :simple :pretty-print true}
                :jar true}
               {:source-path "src/cljs/org/clojars/pgdad/ws_services_admin/passive"
                :compiler {:output-to "resources/public/js/passive.js"
                           :optimizations :simple :pretty-print true}
                :jar true}
               {:source-path "src/cljs/org/clojars/pgdad/ws_services_admin/createpassive"
                :compiler {:output-to "resources/public/js/createpassive.js"
                           :optimizations :simple :pretty-print true}
                :jar true}]}
  :ring {:handler org.clojars.pgdad.ws-sservices-admin.core/app}
  :hooks [leiningen.cljsbuild]
  :main org.clojars.pgdad.ws-services-admin.core)

