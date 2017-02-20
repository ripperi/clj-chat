(ns clj-chat.views
    (:require [re-frame.core :as re-frame]))

(defn content-view []
  [:div {:class "content flex-col"}
   [:div {:class "messages"}]
   [:form {:class "text-wrap"}
    [:div {:class "text-wrap-inner"}
     [:textarea {:class "text-area" :placeholder "Message..."}]]]])

(defn group-view []
  [:div {:class "group"}
   [:div {:class "edge-wrap"}]
   (content-view)
   [:div {:class "edge-wrap"}]])

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div {:class "main flex-row overflow-hidden"}
       [:div {:class "groups"}]
       (group-view)])))
