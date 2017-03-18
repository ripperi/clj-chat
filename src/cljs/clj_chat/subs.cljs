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
         channel (:channel db)
         group (:id (:group db))]
     (filter (fn [msg]
               (and (= (:group msg) group) (= (:channel msg) channel)))
             messages))))

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
 :login-needed?
 (fn [db]
   (:login-needed? db)))
