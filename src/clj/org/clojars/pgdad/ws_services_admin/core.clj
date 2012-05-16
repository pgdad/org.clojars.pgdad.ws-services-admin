(ns org.clojars.pgdad.ws-services-admin.core
  (:require [org.clojars.pgdad.ws-services-admin.load :as loadservice]
            [org.clojars.pgdad.ws-services-admin.active :as activeservice]
            [org.clojars.pgdad.ws-services-admin.passive :as passiveservice]
            [zookeeper :as zk]
            [clj-zoo.serverSession :as srv])
  (:use lamina.core
        aleph.http
        (ring.middleware resource file-info)
        (hiccup core page)))

(def ^:dynamic *keepers* nil)

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

(defn- service-handler [init-f node-f channel]
  (let [servs (init-f)
        ch (:channel @servs)
        ]
    (on-closed channel #(do
                           (println "ACTIVE HANDLER CLIENT CLOSED CHANNEL")
                           (loadservice/close servs)))
    (receive-all channel #(do
                             (println (str "RECEIVED FROM ACTIVE: " %))
                             (println (str " KEEPERS: " *keepers*))
                             (let [z (zk/connect *keepers*)]
                               (node-f z %)
                               (zk/close z))))
    (siphon ch channel)))

(def active-handler (partial service-handler
                             (fn []
                               (activeservice/initialize "localhost/CbbServices" "PROD" "SI"))
                             #(srv/request-passivation %1 %2)
                             ))

(def passive-handler (partial service-handler
                              (fn []
                                (passiveservice/initialize "localhost/CbbServices" "PROD" "SI"))
                              #(srv/request-activation %1 %2)
                              ))

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
      ".act {background:red;} .acted {background:black;}
       .ButtonTooltip { background: #C0C0FF; color: infotext; border: 1px solid infotext; padding: 1px;}"]]
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

(defn -main [keepers & args]
  (def *keepers* keepers)
  (start-http-server app {:port 8080 :websocket true}))
