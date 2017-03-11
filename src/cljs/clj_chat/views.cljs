(ns clj-chat.views
  (:require [re-frame.core :as re-frame]
            [clj-chat.events :as events]
            [reagent.core :refer [atom]]
            [cljs-time.format :as tf :refer [unparse formatter]]
            [cljs-time.coerce :as coerce :refer [from-long to-long to-string]]
            [cljs-time.core :as time :refer [to-default-time-zone]]))

(defn message-view [message]
  [:li.message {:key (:time message)}
   [:span.timestamp (unparse (formatter "yyyy-MM-dd HH:mm") (to-default-time-zone (from-long (:time message))))]
   [:span.msg (:value message)]])

(defn messages-view []
  (let [messages (re-frame/subscribe [:messages])]
    [:ul.messages
     (if-not (empty? @messages)
       (map message-view @messages))]))

(defn content-view []
  (let [value (atom "")
        change-handler (fn [e] (reset! value (-> e .-target .-value)))
        submit-handler (fn [e] (do
                                 (.preventDefault e)
                                 (events/send-msg {:time (str (.getTime (js/Date.))) :value @value})
                                 (reset! value "")))]
    (fn []
      [:div.content.flex-col
       (messages-view)
       [:div.text-wrap
        [:form.text-wrap-inner {:on-submit submit-handler}
         [:input.text-area{:on-change change-handler :placeholder "Message..." :value @value}]
         [:input {:type "submit"}]]]])))

(defn groups-view []
  [:div {:class "groups"}])

(defn group-view []
  [:div.group
   [:div.edge-wrap]
   [content-view]
   [:div.edge-wrap]])

(defn main-panel []
  [:div.main.flex-row.overflow-hidden
   (groups-view)
   (group-view)])
