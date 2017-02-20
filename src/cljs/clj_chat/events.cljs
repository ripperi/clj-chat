(ns clj-chat.events
    (:require [re-frame.core :as re-frame]
              [clj-chat.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(defn send-message [state [_ message]]
  (if (not-empty (:value message))
    (update state :messages conj message)
    state))

(re-frame/reg-event-db :send-message send-message)
