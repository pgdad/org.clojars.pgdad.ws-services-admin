(ns org.clojars.pgdad.ws-services-admin.core
  (:require [org.clojars.pgdad.ws-services-admin.services :as services])
  (:use lamina.core
        aleph.http
        (ring.middleware resource file-info)
        (hiccup core page)))

(def init-messages
  '("add item1",
    ))

(defn load-handler [channel]
  ;; (doseq [m init-messages]
  ;;   (enqueue channel m))
  (let [servs (services/initialize "localhost/CbbServices" "PROD" "SI")
        ch (:channel @servs)
        fch (fork ch)]
    (on-closed channel #(do
                          (println "LOAD HANDLER CLIENT CLOSED CHANNEL")
                          (services/close servs))
               )
    (siphon ch channel)
    (receive-all fch #(println (str "IN FORK: " %)))
    )
)

(defn page []
  (html5
   [:head]
   [:body#thebody
    [:h1 "WebSocket Table"]
    [:table#thetable {:border 0 :cellpadding 3}
     [:thead
      [:tr
       [:th "Region"]
       [:th "Node"]
       [:th "Host"]
       [:th "Load"]]]
     [:tbody#thetablebody]]

    [:h1 "WebSocket PRE"]
    [:h1 "WebSocket Message"]
    (include-js "/js/app.js")]))

(defn sync-app [request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (page)})

(def wrapped-sync-app
  (-> sync-app
      (wrap-resource "public")
      (wrap-file-info)))

(defn app [channel request]
  (println (str "APP: " request))
  (if (:websocket request)
    (load-handler channel)
    (enqueue channel (wrapped-sync-app request))))

(defn -main [& args]
  (start-http-server app {:port 8080 :websocket true}))
