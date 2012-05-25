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

;; map service -> createpassive status (boolean)
(def services (atom {}))

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

(def actTooltipMsg "PUSH ME")

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
           (.setAttribute button "class" (if create-passive? "act" "pas"))
           (.setAttribute cell1 "type" "button")
           (.appendChild cell1 button)
           (.setAttribute button "id" (str "btn" service))
           (goog.events.listen googButton
                               (.-ACTION goog.ui.Component.EventType)
                               #(do
                                  (let [btnClass (.-className button)]
                                    (.log js/console (str "PUSHED: " %&))
                                    (.log js/console (str "BUTTON: " button))
                                    (.log js/console (str "BUTTONG: " btnClass))
                                    (.log js/console (str "GUTTON: " googButton))
                                    (if (= btnClass "act")
                                      (.setAttribute button "class" "pas")
                                      (.setAttribute button "class" "act"))
                                    (.send socket
                                      (str (if (= bntClass "act") "pas " "act ")
                                           (.-value button))))))
           )
         (.setAttribute row "id"  (cstr/replace service "/" ""))
         (resort)
         ))

(defn- update-table
  [service create-passive?]
  (.log js/console (str "UPDATING TABLE FOR: " service))
  (if-let [btn ($$ (str "btn"  service))]
    (let [btnClass (.-class btn)]
      (if (= btnClass "act")
        #_(.setAttribute btn "class" "pas")
        (.log js/console (str "SETTING CLASS TO PAS: " btn))
        #_(.setAttribute btn "class" "act")
        (.log js/console (str "SETTING CLASS TO PAS: " btn))
      ))
     (addrow service create-passive?A)))

(set! (.-onmessage socket)
      #(do (let [msg (.-data %)
                 msg-parts (cstr/split msg splitter)
                 action (first msg-parts)]
             (.log js/console (str "GOT MSG: " msg))
             (cond
              ;; created registration
              (= action "c-r")
              (let [node (second msg-parts)]
                ;; only insert the service if it isn't there already,
                (if-not (contains? @services node)
                  (do
                    (swap! services assoc node false)
                    (update-table node false))))
              
              ;; deleted registration
              (= action "d-r")
              (let [node (second msg-parts)]
                (swap! services dissoc node)
                (rmrow node))
              
              ;; created crepassive
              (= action "c-p")
              (let [node (second msg-parts)]
                (swap! services assoc node true)
                (update-table node true))

              ;; deleted crepassive
              (= action "d-p")
              ;; only set to false if it still exists
              (let [node (second msg-parts )]
                (if (contains? @services node)
                  (do
                    (swap! services assoc node false)
                    (update-table node false))))
              ))))

#_(set! (.-onclose socket) #(do (-> thebody (apptext "SERVER CONN CLOSED."))))
