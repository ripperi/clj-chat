(ns clj-chat.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :messages
 (fn [db]
   (:messages db)))

(re-frame/reg-sub
 :filtered-messages
 (fn [db]
   (let [messages (:messages db)
         direct-messages (:direct-messages db)
         channel (:channel db)
         member (get-in db [:member :id])
         group (:id (:group db))]
     (if-not member
       (filter (fn [msg]
                 (and (= (:group msg) group) (= (:channel msg) channel)))
               messages)
       (filter (fn [msg]
                 (or (= member (get-in msg [:from :id])) (= member (get-in msg [:to :id]))))
               direct-messages)))))

(re-frame/reg-sub
 :background-dim
 (fn [db]
   (:background-dim db)))

(re-frame/reg-sub
 :add-group
 (fn [db]
   (:add-group db)))

(re-frame/reg-sub
 :groups
 (fn [db]
   (:groups db)))

(re-frame/reg-sub
 :group
 (fn [db]
   (:group db)))

(re-frame/reg-sub
 :channel
 (fn [db]
   (:channel db)))

(re-frame/reg-sub
 :member
 (fn [db]
   (:member db)))

(re-frame/reg-sub
 :members
 (fn [db]
   (vals (get-in db [:group :users]))))

(re-frame/reg-sub
 :group-id
 (fn [db]
   (:id (:group db))))

(re-frame/reg-sub
 :group-selected?
 (fn [db]
   (not (nil? (:group db)))))

(re-frame/reg-sub
 :show-content?
 (fn [db]
   (if (or (:channel db) (:member db)) true)))

(re-frame/reg-sub
 :login-needed?
 (fn [db]
   (:login-needed? db)))
