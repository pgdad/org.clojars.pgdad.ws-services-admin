(ns org.clojars.pgdad.ws-services-admin.active.core
  (:require [goog.async.Delay :as Delay]
            [goog.ui.TableSorter :as TableSorter]
            [goog.ui.Tooltip :as Tooltip]
            [goog.ui.Button :as Button]
            [goog.ui.decorate :as decorate]
            [goog.events :as Events]
            [goog.events.EventType :as EventType]
            [goog.object :as gobject]
            [clojure.string :as cstr])
  (:require-macros [org.clojars.pgdad.ws-services-admin.common :as common]))

(def active true)

(common/service-tracking-ws-client)
