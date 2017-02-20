(ns clj-chat.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :refer [atom]]))

(defn message-view [message]
  [:li.message {:key (:time message)} (:value message)])

(defn messages-view []
  (let [messages (re-frame/subscribe [:messages])]
    [:ul.messages
     (if (not (empty? @messages))
       (map message-view @messages))]))

(defn content-view []
  (let [value (atom "")
        change-handler (fn [e] (reset! value (-> e .-target .-value)))
        submit-handler (fn [e] (do
                                 (.preventDefault e)
                                 (re-frame/dispatch [:send-message {:time (str (.getTime (js/Date.))) :value @value}])
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
