(ns clj-chat.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :refer [atom]]))

(defn message-view [message]
  [:li {:class "message"} message])

(defn messages-view []
  (let [messages (re-frame/subscribe [:messages])]
    [:ul {:class "messages"}
     (if (not (empty? @messages))
       (map message-view @messages))]))

(defn content-view []
  (let [value (atom "")
        change-handler (fn [e] (reset! value (-> e .-target .-value)))
        submit-handler (fn [e] (do
                                 (.preventDefault e)
                                 (reset! value "")))]
    (fn []
      [:div {:class "content flex-col"}
       (messages-view)
       [:div {:class "text-wrap"}
        [:form {:class "text-wrap-inner" :on-submit submit-handler}
         [:input {:class "text-area" :on-change change-handler :placeholder "Message..." :value @value}]
         [:input {:type "submit"}]]]])))

(defn groups-view []
  [:div {:class "groups"}])

(defn group-view []
  [:div {:class "group"}
   [:div {:class "edge-wrap"}]
   [content-view]
   [:div {:class "edge-wrap"}]])

(defn main-panel []
  [:div {:class "main flex-row overflow-hidden"}
   (groups-view)
   (group-view)])
