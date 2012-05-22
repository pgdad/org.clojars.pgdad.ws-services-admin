(ns org.clojars.pgdad.ws-services-admin.core
  (:require [org.clojars.pgdad.ws-services-admin.load :as loadservice]
            [org.clojars.pgdad.ws-services-admin.active :as activeservice]
            [org.clojars.pgdad.ws-services-admin.passive :as passiveservice]
            [zookeeper :as zk]
            [clj-zoo.serverSession :as srv])
  (:use lamina.core
        aleph.http
        (ring.middleware resource file-info)
        (hiccup core page))
  (gen-class))

(declare zookeepers)

(defn load-handler [channel]
  (let [servs (loadservice/initialize zookeepers)
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
                           (println "HANDLER CLIENT CLOSED CHANNEL")
                           (loadservice/close servs)))
    (receive-all channel #(do
                             (let [z (zk/connect zookeepers)]
                               (node-f z %)
                               (zk/close z))))
    (siphon ch channel)))

(def active-handler (partial service-handler
                             (fn []
                               (activeservice/initialize zookeepers))
                             #(srv/request-passivation %1 %2)
                             ))

(def passive-handler (partial service-handler
                              (fn []
                                (passiveservice/initialize zookeepers))
                              #(srv/request-activation %1 %2)
                              ))

(defn loadpage []
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

    [:h1 "WebSocket Message"]
    (include-js "/js/load.js")]))

(defn active-passive [page]
  (html5
   [:head
     [:style {:type "text/css"}
      ".act {background:red;} .acted {background:black;}
       .ButtonTooltip { background: #C0C0FF; color: infotext; border: 1px solid infotext; padding: 1px;}"]]
   [:body#thebody
    [:h1 (str "Services " page)]
    [:table#thetable {:border 0 :cellpadding 3}
     [:thead
      [:tr
       [:th "Region"]
       [:th "Service"]
       [:th "Major"]
       [:th "Minor"]
       [:th "Micro"]
       [:th "URL"]
       ]]
     [:tbody#thetablebody]]

    (include-js (str "/js/" page ".js"))]))

(def active (partial active-passive "active"))

(def passive (partial active-passive "passive"))

(defn sync-app [f request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (f)})

(def wrapped-load-app
  (-> (partial sync-app loadpage)
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
  (def zookeepers keepers)
  (try (start-http-server app {:port 8080 :websocket true})
       (catch Exception ex (do
                             (println (str "EXCEPTION: " ex))
                             (.printStackTrace ex)))))
