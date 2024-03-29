(ns org.clojars.pgdad.ws-services-admin.common)

;; Common functionality for both active and passive

(defmacro service-tracking-ws-client []
  '(do
     (def surl js/window.location.href)

     (def wsurl (.replace surl "http:" "ws:"))

     (def socket (new js/window.WebSocket wsurl))

     (defn $ [selector]
       (.querySelector js/document (name selector)))

     (defn $$ [id]
       (let [selector (str "#" id)]
         (.querySelector js/document selector )))

     (def thebody ($ :#thebody))

     (def thetable ($ :#thetable))

     (def thetablebody ($ :#thetablebody))

     (def sortingtable (goog.ui.TableSorter.))

     (do
       (.decorate sortingtable thetable)
       (.setSortFunction sortingtable 0 TableSorter/alphaSort)
       (.setSortFunction sortingtable 1 TableSorter/alphaSort)
       (.setSortFunction sortingtable 2 TableSorter/numericSort)
       (.setSortFunction sortingtable 3 TableSorter/numericSort)
       (.setSortFunction sortingtable 4 TableSorter/numericSort)
       (.setSortFunction sortingtable 5 TableSorter/alphaSort)
       (.sort sortingtable 0 false)
       )

     (defn apptext [c txt ]
       (.appendChild c (.createTextNode js/document txt))
       (.appendChild c (.createElement js/document "br")))

     (defn- resort []
       (let [sortcolumn (.getSortColumn sortingtable)
             isreversed (.isSortReversed sortingtable)]
         (.sort sortingtable sortcolumn isreversed)))

     (def delay (atom nil))

     (def resortDelayMS 100)
     
     (defn- doDelayedResort
       []
       (if-not @delay
         (swap! delay (fn [& args] (goog.async.Delay. resort resortDealyMS)))
         (.start @delay))
       )

     (defn- element-id [node]
       (cstr/replace node "/" ""))

     (def actTooltipMsg
       (str "Click on this button to request "
            (if active "passivation" "activation"
                " of the service.")))

     (defn addrow [tbl region node service major minor micro url]
       (let [l (.-length (.-rows tbl))
             row (.insertRow tbl l)
             cell0 (.insertCell row 0)
             cell1 (.insertCell row 1)
             cell2 (.insertCell row 2)
             cell3 (.insertCell row 3)
             cell4 (.insertCell row 4)
             cell5 (.insertCell row 5)
             cell6 (.insertCell row 6)]
         (.appendChild cell0 (.createTextNode js/document region))
         (.appendChild cell1 (.createTextNode js/document service))
         (.appendChild cell2 (.createTextNode js/document major))
         (.appendChild cell3 (.createTextNode js/document minor))
         (.appendChild cell4 (.createTextNode js/document micro))
         (.appendChild cell5 (.createTextNode js/document url))
         (let [button (.createElement js/document "button")
               btnTooltip (goog.ui.Tooltip. button actTooltipMsg)
               googButton (goog.ui.Button. button)]
           (set! (.-className btnTooltip) "ButtonTooltip")
           (.decorate googButton button)
           (.setAttribute button "value" node)
           (.setAttribute button "class" "act")
           (.setAttribute cell6 "type" "button")
           (.appendChild cell6 button)
           (goog.events.listen googButton
                               (.-ACTION goog.ui.Component.EventType)
                               #(do
                                  (.setAttribute button "class" "acted")
                                  (.send socket (.getAttribute button "value"))))
           )
         (.setAttribute row "id"  (cstr/replace node "/" ""))
         (doDelayedResort)
         ))

     (defn rmnode [node]
       (let [elementid (element-id node)
             rmrow ($$ elementid)]
         (-> thetablebody (.removeChild rmrow))
         )
       (doDelayedResort)
       )

     (set! (.-onmessage socket)
           #(do (let [msg (.-data %)
                      msg-parts (cstr/split msg (re-pattern " "))
                      action (first msg-parts)]
                  (cond
                   ;; created
                   (= action "c")
                   (let [[_ region node service major minor micro url] msg-parts]
                     (-> thetablebody (addrow
                                       region
                                       node
                                       service
                                       major
                                       minor
                                       micro
                                       url)))
                   ;; deleted
                   (= action "d")
                   (let [node (nth msg-parts 2)]
                     (rmnode node))
                   ))))

     (set! (.-onclose socket)
           #(do
              (-> thebody (apptext "SERVER CONN CLOSED."))
              (doseq [rmrow (range (.-length (.-rows thetablebody)))]
                (.deleteRow thetablebody 0))))
  ))
