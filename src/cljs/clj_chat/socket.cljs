(ns clj-chat.socket
  (:require [re-frame.core :refer [dispatch]]
            [cljs.core.async :as async :refer (<! >! put! chan)]
            [taoensso.sente :as sente :refer (cb-success?)])
  (:require-macros [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket! "/chsk" {:type :auto})]
  (def chsk       chsk)
  (def ch-chsk    ch-recv)
  (def chsk-send! send-fn)
  (def chsk-state state))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  (comp first :?data))

(defmethod -event-msg-handler
  :default
  [{:keys [id]}]
  (println id))

(defmethod -event-msg-handler
  :clj-chat.handler/unmatched-event
  [{:keys [?data]}]
  (println (str "UNMATCHED EVENT: " ?data)))

(defmethod -event-msg-handler
  :clj-chat.handler/message
  [{:keys [id ?data]}]
  (println id)
  (println ?data))

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
           ch-chsk -event-msg-handler)))
(defonce _start-once (start-router!))
