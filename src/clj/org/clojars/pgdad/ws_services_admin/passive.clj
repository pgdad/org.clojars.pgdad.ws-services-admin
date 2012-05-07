(ns org.clojars.pgdad.ws-services-admin.passive
  (:require [zookeeper :as zk]
            [clj-zoo-watcher.multi :as mw]
            [clojure.tools.logging :as log]
            [lamina.core :as lc])
  (:gen-class))

(defn- service-created
  [client channel region _ node]
  (let [data (String. (:data (zk/data client node)) "UTF-8")]
    (lc/enqueue channel (str "c " region " " node " " data "\n"))))

(defn- service-deleted
  [channel region _ node]
  (lc/enqueue channel (str "d " region " " node "\n")))


(defn initialize
  [keepers env app]
  (let [client (zk/connect keepers)
        ch (lc/channel* :transactional? true)
        data-ref (ref {})
        servers-root (str "/"  env "/" app "/servers")
        mw (mw/child-watchers
            client servers-root
            data-ref
            (fn [event] (println (str "CONNECTION EVENT: " event)))
            (fn [region data-ref dir-node] nil)
            (fn [region data-ref dir-node] nil)
            (partial service-created client ch)
            (partial service-deleted ch)
            #(log/spy :debug %4)
            data-ref)]
    (ref {:channel ch :m-ref mw})))

(defn close
  [m-ref]
  (let [mw (:m-ref @m-ref)
        connection (:connection @mw)]
    (zk/close connection)))