(ns org.clojars.pgdad.ws-services-admin.core
  (:require [org.clojars.pgdad.ws-services-admin.load :as loadservice]
            [org.clojars.pgdad.ws-services-admin.active :as activeservice]
            [org.clojars.pgdad.ws-services-admin.passive :as passiveservice])
  (:use lamina.core
        aleph.http
        (ring.middleware resource file-info)
        (hiccup core page)))

(defn load-handler [channel]
  (let [servs (loadservice/initialize "localhost/CbbServices" "PROD" "SI")
        ch (:channel @servs)
        ]
    (on-closed channel #(do
                          (println "LOAD HANDLER CLIENT CLOSED CHANNEL")
                          (loadservice/close servs))
               )
    (siphon ch channel)
    )
)

(defn active-handler [channel]
  (let [servs (activeservice/initialize "localhost/CbbServices" "PROD" "SI")
        ch (:channel @servs)
        ]
    (on-closed channel #(do
                          (println "ACTIVE HANDLER CLIENT CLOSED CHANNEL")
                          (loadservice/close servs))
               )
    (siphon ch channel)
    )
)

(defn passive-handler [channel]
  (let [servs (passiveservice/initialize "localhost/CbbServices" "PROD" "SI")
        ch (:channel @servs)
        ]
    (on-closed channel #(do
                          (println "PASSIVE HANDLER CLIENT CLOSED CHANNEL")
                          (loadservice/close servs))
               )
    (siphon ch channel)
    )
)


(defn load []
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
    (include-js "/js/load.js")]))

(defn passive []
  (html5
   [:head]
   [:body#thebody
    [:h1 "Passivated Services"]
    [:table#thetable {:border 0 :cellpadding 3}
     [:thead
      [:tr
       [:th "Region"]
       [:th "Node"]
       [:th "Service"]
       [:th "Major"]
       [:th "Minor"]
       [:th "Micro"]
       [:th "URL"]
       ]]
     [:tbody#thetablebody]]

    [:h1 "WebSocket Message"]
    (include-js "/js/passive.js")]))

(defn active []
  (html5
   [:head
     [:style {:type "text/css"}
        ".act {background:red;} .acted {background:black;}"]]
   [:body#thebody
    [:h1 "Active Services"]
    [:table#thetable {:border 0 :cellpadding 3}
     [:thead
      [:tr
       [:th "Region"]
       [:th "Node"]
       [:th "Service"]
       [:th "Major"]
       [:th "Minor"]
       [:th "Micro"]
       [:th "URL"]
       ]]
     [:tbody#thetablebody]]

    [:h1 "WebSocket Message"]
    (include-js "/js/active.js")]))

(defn sync-app [f request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (f)})

(def wrapped-load-app
  (-> (partial sync-app load)
      (wrap-resource "public")
      (wrap-file-info)))

(def wrapped-passive-app
  (-> (partial sync-app passive)
      (wrap-resource "public")
      (wrap-file-info)))

(def wrapped-active-app
  (-> (partial sync-app active)
      (wrap-resource "public")
      (wrap-file-info)))

(defn app [channel request]
  (let [uri (:uri request)]
    (println (str "URI: " uri))
    (if (:websocket request)
      (cond
       ;; load
       (= uri "/load")
       (load-handler channel)
       ;; active services
       (= uri "/active")
       (active-handler channel)
       ;; passive services
       (= uri "/passive")
       (passive-handler channel)
        )
      
      
      (cond
       ;; load
       (= uri "/load")
       (enqueue channel (wrapped-load-app request))
       ;; active services
       (= uri "/active")
       (enqueue channel (wrapped-active-app request))
       ;; passive services
       (= uri "/passive")
       (enqueue channel (wrapped-passive-app request))
       :else
       (enqueue channel (wrapped-load-app request))
       ))))

(defn -main [& args]
  (start-http-server app {:port 8080 :websocket true}))
