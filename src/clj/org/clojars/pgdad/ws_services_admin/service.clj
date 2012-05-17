(ns org.clojars.pgdad.ws-services-admin.service
  (:require [zookeeper :as zk]
            [clj-zoo-watcher.multi :as mw]
            [clojure.tools.logging :as log]
            [lamina.core :as lc]
            [clojure.string :as cstr])
  (:gen-class))

;; common functionality for both passive.clj and active.clj

(defn- service-created
  [client channel region _ node]
  (let [data (String. (:data (zk/data client node)) "UTF-8")
        [_ _ url] (clojure.string/split-lines data)
        _ (println (cstr/split node (re-pattern "/")))
        [_ _ _ service major minor micro] (cstr/split node (re-pattern "/"))
        msg (str "c " region " " node " "
                 service " " major " "
                 minor " " micro " " url "\n")]
    (lc/enqueue channel msg)))

(defn- service-deleted
  [channel region _ node]
  (lc/enqueue channel (str "d " region " " node "\n")))


(defn initialize
  [keepers service-path]
  (let [client (zk/connect keepers)
        ch (lc/channel* :transactional? true)
        data-ref (ref {})
        servers-root (str "/" service-path)
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

