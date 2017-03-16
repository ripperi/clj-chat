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

(defn login [username]
  (socket/chsk-send! [:update/login username]))

(re-frame/reg-event-db :rec-msg rec-msg)

(re-frame/reg-event-db
 :toggle-background
 (fn [state]
   (update state :background-dim not)))

(re-frame/reg-event-db
 :toggle-add-group
 (fn [state]
   (update state :add-group not)))

(re-frame/reg-event-db
 :update-groups
 (fn [state [_ updated-groups]]
   (assoc state :groups updated-groups)))

(re-frame/reg-event-db
 :select-group
 (fn [state [_ group]]
   (assoc state :group group)))

(re-frame/reg-event-db
 :select-channel
 (fn [state [_ channel]]
   (assoc state :channel channel)))

(re-frame/reg-event-db
 :update-login-need
 (fn [state [_ bool]]
   (assoc state :login-needed? bool)))
