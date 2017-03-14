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
