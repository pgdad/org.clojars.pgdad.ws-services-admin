(ns org.clojars.pgdad.ws-services-admin.active.core
  (:require [goog.ui.TableSorter :as TableSorter]
            [clojure.string :as cstr])
  (:require-macros [org.clojars.pgdad.ws-services-admin.common :as common]))

(common/service-tracking-ws-client true)
