(ns org.clojars.pgdad.ws-services-admin.createpassive.createpassive
  (:require [goog.ui.TableSorter :as TableSorter]
            [clojure.string :as cstr]
            [goog.ui.Tooltip :as Tooltip]
            [goog.ui.Button :as Button]
            [goog.ui.decorate :as decorate]
            [goog.events :as Events]
            [goog.events.EventType :as EventType]
            [goog.object :as gobject]))

(def surl js/window.location.href)

(def wsurl (.replace surl "http:" "ws:"))

(def socket (new js/window.WebSocket wsurl))

(def splitter (re-pattern " "))

(defn $ [selector]
  (.querySelector js/document (name selector)))

(defn $$ [id]
  (let [selector (str "#" id)]
    (.querySelector js/document selector )))

(def thetable ($ :#thetable))

(def thetablebody ($ :#thetablebody))

(def sortingtable (goog.ui.TableSorter.))

(do
  (.decorate sortingtable thetable)
  (.setSortFunction sortingtable 0 TableSorter/alphaSort)
  (.sort sortingtable 0 true)
  )

(defn resort []
  (let [sortcolumn (.getSortColumn sortingtable)
        isreversed (.isSortReversed sortingtable)]
    (.sort sortingtable sortcolumn isreversed)))

(defn- element-id [node]
  (cstr/replace node "/" ""))


(defn rmnode [node]
  (let [elementid (element-id node)
        rmrow ($$ elementid)]
    (-> thetablebody (.removeChild rmrow))
    )
  (resort))

(def actTooltipMsg "Push to toggle between green and red.\nWhen green, new service instances are created passive.\nWhen red, new service instances are created active.")

(defn addrow [service create-passive?]
       (let [l (.-length (.-rows thetablebody))
             row (.insertRow thetablebody l)
             cell0 (.insertCell row 0)
             cell1 (.insertCell row 1)]
         (.appendChild cell0 (.createTextNode js/document service))
         (let [button (.createElement js/document "button")
               btnTooltip (goog.ui.Tooltip. button actTooltipMsg)
               googButton (goog.ui.Button. button)]
           (set! (.-className btnTooltip) "ButtonTooltip")
           (.decorate googButton button)
           (.setAttribute button "value" service)
           (.setAttribute button "class" (if create-passive? "pas" "act"))
           (.setAttribute cell1 "type" "button")
           (.appendChild cell1 button)
           (.setAttribute button "id" (str "btn" service))
           (goog.events.listen googButton
                               (.-ACTION goog.ui.Component.EventType)
                               #(do
                                  (let [btnClass (.-className button)]
                                    (.send socket
                                           (str btnClass " " (.-value button)))
                                    (if (= btnClass "act")
                                      (.setAttribute button "class" "pas")
                                      (.setAttribute button "class" "act"))
                                    )))
           )
         (.setAttribute row "id"  (cstr/replace service "/" ""))
         (resort)
         ))

(defn- update-table
  [service create-passive?]
  (if-let [btn ($$ (str "btn"  service))]
    (if create-passive?
      (.setAttribute btn "class" "pas")
      (.setAttribute btn "class" "act"))
    (addrow service create-passive?)))

(set! (.-onmessage socket)
      #(do (let [msg (.-data %)
                 msg-parts (cstr/split msg splitter)
                 action (first msg-parts)]
             (cond
              ;; created registration
              (= action "c-r")
              (let [node (second msg-parts)]
                ;; only insert the service if it isn't there already,
                (update-table node false))
              
              ;; deleted registration
              (= action "d-r")
              (let [node (second msg-parts)]
                (rmrow node))
              
              ;; created crepassive
              (= action "c-p")
              (let [node (second msg-parts)]
                (update-table node true))

              ;; deleted crepassive
              (= action "d-p")
              ;; only set to false if it still exists
              (let [node (second msg-parts )]
                (update-table node false))
              ))))

#_(set! (.-onclose socket) #(do (-> thebody (apptext "SERVER CONN CLOSED."))))
