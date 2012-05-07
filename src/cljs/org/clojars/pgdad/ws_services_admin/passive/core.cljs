(ns org.clojars.pgdad.ws-services-admin.passive.core
  (:require [goog.ui.TableSorter :as TableSorter]
            [clojure.string :as cstr]))

(def surl js/window.location.href)

(def wsurl (.replace surl "http:" "ws:"))

(def socket (new js/window.WebSocket wsurl))

(def splitter (re-pattern " "))

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
  (.setSortFunction sortingtable 2 TableSorter/alphaSort)
  (.setSortFunction sortingtable 3 TableSorter/alphaSort)
  (.sort sortingtable 0 false)
  )

(defn apptext [c txt ]
  (.appendChild c (.createTextNode js/document txt))
  (.appendChild c (.createElement js/document "br")))

(defn resort []
  (let [sortcolumn (.getSortColumn sortingtable)
        isreversed (.isSortReversed sortingtable)]
    (.sort sortingtable sortcolumn isreversed)))

(defn- element-id [node]
  (cstr/replace node "/" ""))

(defn addrow [tbl region node host load]
  (let [l (.-length (.-rows tbl))
        row (.insertRow tbl l)
        cell0 (.insertCell row 0)
        cell1 (.insertCell row 1)
        cell2 (.insertCell row 2)
        cell3 (.insertCell row 3)]
    (.appendChild cell0 (.createTextNode js/document region))
    (.appendChild cell1 (.createTextNode js/document node))
    (.appendChild cell2 (.createTextNode js/document host))
    ;; tag the load node so that we can easily find it later
    (let [load-node (.createTextNode js/document load)]
      (.setAttribute cell3 "id" (str "load" (element-id node)))
      (.appendChild cell3 load-node))
    (.setAttribute row "id"  (cstr/replace node "/" ""))
    (resort)
  ))

(defn rmnode [node]
  (let [elementid (element-id node)
        rmrow ($$ elementid)]
    (-> thetablebody (.removeChild rmrow))
   )
  (resort)
)

(defn updateload [node load]
  (let [elementid (str "load" (element-id node))
        load-node ($$ elementid)
        load-text-node (.-firstChild load-node)]
    (set! (.-nodeValue load-text-node) load))
  (resort)
  )

(set! (.-onmessage socket)
      #(do (let [msg (.-data %)
                 msg-parts (cstr/split msg splitter)
                 action (first msg-parts)]
             (if-not thetable
               (-> thebody (apptext "THE TABLE IS NOT DEFINED.")))
             (cond
              ;; created
              (= action "c")
              (let [node-data-part (cstr/split-lines (nth msg-parts 3))
                    host (nth node-data-part 2)
                    load (nth node-data-part 1)]
                (-> thetablebody (addrow
                                  (nth msg-parts 1)
                                  (nth msg-parts 2)
                                  host
                                  load)))
              ;; load changed
              (= action "l")
              (let [node (nth msg-parts 2)
                    node-data-part (cstr/split-lines (nth msg-parts 3))
                    load (nth node-data-part 1)]
                (updateload node load)) 
              ;; deleted
              (= action "d")
              (let [node (nth msg-parts 2)]
                (rmnode node))
              ))))

(set! (.-onclose socket) #(do (-> thebody (apptext "SERVER CONN CLOSED."))))