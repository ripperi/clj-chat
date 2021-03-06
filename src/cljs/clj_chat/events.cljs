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

(defn send-direct-message [msg]
  (if (seq (:value msg))
    (socket/chsk-send! [::direct-message msg])))

(defn rec-msg [state [_ msg]]
  (if (seq (:value msg))
    (update state :messages conj msg)
    state))

(defn rec-dir-msg [state [_ msg]]
    (update state :direct-messages conj msg))

(defn add-group [name]
  (if (seq name)
    (socket/chsk-send! [:room/add name])))

(defn add-channel [group channel]
  (if (and (seq group) (seq channel))
    (socket/chsk-send! [:add/channel {:group group :channel channel}])))

(defn add-member [group member]
  (if (and (seq group) (seq member))
    (socket/chsk-send! [:add/member {:group group :member member}])))

(defn login [username]
  (socket/chsk-send! [:update/login username]))

(re-frame/reg-event-db :rec-msg rec-msg)

(re-frame/reg-event-db :rec-dir-msg rec-dir-msg)

(re-frame/reg-event-db
 :toggle-modals-off
 (fn [state]
   (-> (assoc state :background-dim false)
       (assoc :add-member false)
       (assoc :add-channel false)
       (assoc :add-group false))))

(re-frame/reg-event-db
 :toggle-modal
 (fn [state [_ modal]]
   (-> (update state modal not)
       (update :background-dim not))))

;; (re-frame/reg-event-db
;;  :update-groups
;;  (fn [state [_ updated-groups]]
;;    (assoc state :groups updated-groups)))

(re-frame/reg-event-db
 :update-groups
 (fn [state [_ updated-groups]]
   (let [group-selected? (not (nil? (:group state)))
         updated-groups (assoc state :groups updated-groups)
         selected-group-id (get-in state [:group :id])]
     (if group-selected?
       (assoc updated-groups :group (some #(when (= (:id %) selected-group-id) %) (:groups updated-groups)))
       updated-groups))))

(re-frame/reg-event-db
 :select-group
 (fn [state [_ group]]
   (assoc state :group group)))

(re-frame/reg-event-db
 :select-channel
 (fn [state [_ channel]]
   (assoc state :channel channel)))

(re-frame/reg-event-db
 :select-member
 (fn [state [_ member]]
   (assoc state :member member)))

(re-frame/reg-event-db
 :update-login-need
 (fn [state [_ bool]]
   (assoc state :login-needed? bool)))
