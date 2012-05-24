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
  [channel _ _ node]
  (println (str "CLI REG CREATED: " node))
  (let [node-parts (clojure.string/split node split-pattern)]
    (if (= 2 (count node-parts))
      (println (str "- SENDING: " "c-r " (second node-parts)))
      (lc/enqueue "c-r " (second node-parts)))))

(defn- cli-reg-deleted
  [channel _ _ node]
  (let [node-parts (clojure.string/split node split-pattern)]
    (if (= 2 (count node-parts))
      (lc/enqueue "d-r " (second node-parts)))))

(defn- crepassive-created
  [channel _ _ node]
  (lc/enqueue (str "c-p" (-> node (.replaceFirst "/" "")))))

(defn- crepassive-deleted
  [channel _ _ node]
  (lc/enqueue (str "d-p " (-> node (.replaceFirst "/" "")))))

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

