(ns clj-chat.views
    (:require [re-frame.core :as re-frame]))

(defn main-panel []
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div {:class "main"}
       [:div {:class "rooms"}]
       [:div {:class "room"}
        [:div {:class "room-edge"}]
        [:div {:class "chat"}]
        [:div {:class "room-edge"}]]])))
