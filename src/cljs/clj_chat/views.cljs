(ns clj-chat.views
    (:require [re-frame.core :as re-frame]))

(defn message-view [message]
  [:li {:class "message"} message])

(defn messages-view []
  (let [messages (re-frame/subscribe [:messages])]
    [:ul {:class "messages"}
     (if (not (empty? @messages))
       (map message-view @messages))]))

(defn content-view []
  [:div {:class "content flex-col"}
   (messages-view)
   [:form {:class "text-wrap"}
    [:div {:class "text-wrap-inner"}
     [:textarea {:class "text-area" :placeholder "Message..."}]]]])

(defn groups-view []
  [:div {:class "groups"}])

(defn group-view []
  [:div {:class "group"}
   [:div {:class "edge-wrap"}]
   (content-view)
   [:div {:class "edge-wrap"}]])

(defn main-panel []
  [:div {:class "main flex-row overflow-hidden"}
   (groups-view)
   (group-view)])
