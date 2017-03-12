(ns clj-chat.events
    (:require [re-frame.core :as re-frame]
              [clj-chat.db :as db]
              [clj-chat.socket :as socket]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(defn send-msg [msg]
  (if (seq (:value msg))
    (socket/chsk-send! [::message msg])))

(defn rec-msg [state [_ msg]]
  (if (seq (:value msg))
    (update state :messages conj msg)
    state))

(defn add-group [name]
  (if (seq name)
    (socket/chsk-send! [:room/add name])))

(re-frame/reg-event-db :rec-msg rec-msg)

(re-frame/reg-event-db
 :toggle-background
 (fn [state]
   (update state :background-dim not)))

(re-frame/reg-event-db
 :toggle-add-group
 (fn [state]
   (update state :add-group not)))
