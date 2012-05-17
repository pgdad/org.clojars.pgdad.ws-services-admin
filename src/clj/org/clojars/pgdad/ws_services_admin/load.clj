(ns org.clojars.pgdad.ws-services-admin.load
  (:require [zookeeper :as zk]
            [clj-zoo-watcher.multi :as mw]
            [clojure.tools.logging :as log]
            [lamina.core :as lc])
  (:gen-class))

(defn- server-created
  [client channel region _ node]
  (let [data (String. (:data (zk/data client node)) "UTF-8")]
    (lc/enqueue channel (str "c " region " " node " " data "\n"))))

(defn- server-deleted
  [channel region _ node]
  (lc/enqueue channel (str "d " region " " node "\n")))


(defn- server-data-changed
  [channel region _ node data]
  (lc/enqueue channel (str "l " region " " node " " (String. data "UTF-8"))))

(defn initialize
  [keepers]
  (let [client (zk/connect keepers)
        ch (lc/channel* :transactional? true)
        data-ref (ref {})
        servers-root "/servers"
        mw (mw/child-watchers
            client servers-root
            data-ref
            (fn [event] (println (str "CONNECTION EVENT: " event)))
            (fn [region data-ref dir-node] nil)
            (fn [region data-ref dir-node] nil)
            (partial server-created client ch)
            (partial server-deleted ch)
            (partial server-data-changed ch)
            data-ref)]
    (ref {:channel ch :m-ref mw})))

(defn close
  [m-ref]
  (let [mw (:m-ref @m-ref)
        connection (:connection @mw)]
    (zk/close connection)))