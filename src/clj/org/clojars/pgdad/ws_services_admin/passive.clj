(ns org.clojars.pgdad.ws-services-admin.passive
  (:require [zookeeper :as zk]
            [clj-zoo-watcher.multi :as mw]
            [clojure.tools.logging :as log]
            [lamina.core :as lc]
            [clojure.string :as cstr]
            [org.clojars.pgdad.ws-services-admin.service :as service])
  (:gen-class))

(defn initialize
  [keepers]
  (service/initialize keepers "passiveservices"))

(defn close
  [m-ref]
  (service/close m-ref))

