(ns org.clojars.pgdad.ws-services-admin.createpassive
  (:require [zookeeper :as zk]
            [clj-zoo-watcher.core :as wc]
            [clojure.tools.logging :as log]
            [lamina.core :as lc]
            [clojure.string :as cstr]
            [org.clojars.pgdad.ws-services-admin.service :as service])
  (:gen-class))

(def split-pattern #"/")

(defn- cli-reg-created
  [channel _ node]
  (println (str "CRE PASS CLI REG CREATED: " node))
  (let [node-parts (clojure.string/split node split-pattern)]
    (println (str "CRE PASS CLI REG CRE PARTS: " node-parts))
    (if (= 3 (count node-parts))
      (do
        (println (str "- SENDING: " "c-r " (nth node-parts 2)))
        (lc/enqueue channel (str "c-r " (nth node-parts 2)))))))

(defn- cli-reg-deleted
  [channel _ node]
  (println (str "CRE PASS CLI REG DELETED: " node))
  (let [node-parts (clojure.string/split node split-pattern)]
    (if (= 3 (count node-parts))
      (lc/enqueue channel (str "d-r " (nth node-parts 2))))))

(defn- crepassive-created
  [channel _ node]
  (lc/enqueue channel (str "c-p " (-> node (.replaceFirst "/createpassive/" "")))))

(defn- crepassive-deleted
  [channel  _ node]
  (lc/enqueue channel (str "d-p " (-> node (.replaceFirst "/createpassive/" "")))))

(defn initialize
  [keepers]
  (println (str "INITIALIZING CREPASSIVE"))
  (let [client (zk/connect keepers)
        ch (lc/channel* :transactional? true)
        data-ref (ref {})
        reg-watcher
        (wc/watcher
         client "/clientregistrations"
         (fn [event] (println (str "CONNECTION EVENT: " event)))
         (partial cli-reg-created ch)
         (partial cli-reg-deleted ch)
         (fn [& rest] nil)
         (fn [& rest] nil)
         #(log/spy :debug %4)
         data-ref)
        createpassive-watcher
        (wc/watcher
         client "/createpassive"
         (fn [event] (println (str "CONNECTION EVENT: " event)))
         (partial crepassive-created ch)
         (partial crepassive-deleted ch)
         (fn [& rest] nil)
         (fn [& rest] nil)
         #(log/spy :debug %4)
         data-ref)]
    (ref {:channel ch
          :connection client
          :reg-watcher-ref reg-watcher
          :create-passive-watcher-ref createpassive-watcher})))

(defn close
  [cp-ref]
  (let [connection (:connection @cp-ref)]
    (zk/close connection)))

