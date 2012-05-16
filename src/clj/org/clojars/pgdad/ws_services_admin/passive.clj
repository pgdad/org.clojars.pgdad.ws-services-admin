(ns org.clojars.pgdad.ws-services-admin.passive
  (:require [zookeeper :as zk]
            [clj-zoo-watcher.multi :as mw]
            [clojure.tools.logging :as log]
            [lamina.core :as lc]
            [clojure.string :as cstr]
            [org.clojars.pgdad.ws-services-admin.service :as service])
  (:gen-class))

(defn initialize
  [keepers env app]
  (service/initialize keepers "passiveservices" env app))

(defn close
  [m-ref]
  (service/close m-ref))

