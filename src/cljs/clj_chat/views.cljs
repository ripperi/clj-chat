(ns clj-chat.views
  (:require [re-frame.core :as re-frame]
            [clj-chat.events :as events]
            [reagent.core :refer [atom]]
            [cljs-time.format :as tf :refer [unparse formatter]]
            [cljs-time.coerce :as coerce :refer [from-long to-long to-string]]
            [cljs-time.core :as time :refer [to-default-time-zone]]
            [clojure.string :as str]))

(defn background-dim []
  (if @(re-frame/subscribe [:background-dim])
    [:div.background-dim {:on-click #(do (re-frame/dispatch [:toggle-background])
                                         (re-frame/dispatch [:toggle-add-group]))}]
    [:div.background-dim.hidden]))

(defn add-group []
  (if @(re-frame/subscribe [:add-group])
    (let [value (atom "")
          change-handler (fn [e] (reset! value (-> e .-target .-value)))
          submit-handler (fn [e] (do
                                   (.preventDefault e)
                                   (events/add-group @value)
                                   (re-frame/dispatch [:toggle-background])
                                   (re-frame/dispatch [:toggle-add-group])))]
        [:div.modal.top-to-bottom
         [:form.add-group-wrap {:on-submit submit-handler}
          [:span.modal-title "Create Group"]
          [:input.add-group-name {:on-change change-handler :placeholder "Name"}]
          [:input.btn-big {:type "submit"}]]])))

(defn message-view [message]
  [:li.message {:key (:time message)}
   [:span.timestamp (unparse (formatter "yyyy-MM-dd HH:mm") (to-default-time-zone (from-long (:time message))))]
   [:span.msg (:value message)]])

(defn messages-view []
  (let [messages (re-frame/subscribe [:filtered-messages])]
    [:ul.messages
     (map message-view @messages)]))

(defn content-view []
  (let [value (atom "")
        channel-name (re-frame/subscribe [:channel])
        show-content? (re-frame/subscribe [:show-content?])
        group-id (re-frame/subscribe [:group-id])
        change-handler (fn [e] (reset! value (-> e .-target .-value)))
        submit-handler (fn [e] (do
                                 (.preventDefault e)
                                 (events/send-msg {:time (str (.getTime (js/Date.)))
                                                   :value @value
                                                   :channel @channel-name
                                                   :group @group-id})
                                 (reset! value "")))]
    (fn []
      (if @show-content?
        [:div.content.flex-col
         (messages-view)
         [:div.text-wrap
          [:form.text-wrap-inner {:on-submit submit-handler}
           [:input.text-area {:on-change change-handler :placeholder "Message..." :value @value}]
           [:input {:type "submit"}]]]]
        [:div.content.flex-col]))))

(defn select-group [self]
  (let [on-click #(do (re-frame/dispatch [:select-group self])
                      (re-frame/dispatch [:select-channel nil])
                      (re-frame/dispatch [:select-member nil]))
        group (re-frame/subscribe [:group])
        class (if (= @group self)
                 :li.selected.overflow-hidden
                 :li.select-group.overflow-hidden)]
    [class {:on-click on-click :key (:id self)}
     (as-> self s
       (:name s)
       (str/split s #" ")
       (map first s)
       (str/join s)
       (str/upper-case s))]))

(defn channel [channel]
  (let [on-click #(do (re-frame/dispatch [:select-channel channel])
                      (re-frame/dispatch [:select-member nil]))
        active-channel (re-frame/subscribe [:channel])
        class (if (= channel @active-channel)
                :li.channel.active
                :li.channel)]
    [class {:on-click on-click :key channel} channel]))

(defn channels []
  (let [group (re-frame/subscribe [:group])]
    [:div.edge-wrap
     [:div.group-name (:name @group)]
     [:ul.channels (doall (map channel (:channels @group)))]]))

(defn member [member]
  (let [on-click #(do (re-frame/dispatch [:select-member member])
                      (re-frame/dispatch [:select-channel nil]))
        active-member (re-frame/subscribe [:member])
        class (if (= member @active-member)
                :li.channel.active
                :li.channel)]
    [class {:on-click on-click :key (:id member)} (:name member)]))

(defn members []
  (let [members (re-frame/subscribe [:members])]
    [:div
     [:div.group-name "Members"]
     [:ul.channels (doall (map member @members))]]))

(defn groups-view []
  (let [toggle-background #(do (re-frame/dispatch [:toggle-background])
                               (re-frame/dispatch [:toggle-add-group]))
        groups (re-frame/subscribe [:groups])]
    [:div.groups
     [:ul.groups (doall (map select-group @groups))]
     [:div.add-group {:type "button" :on-click toggle-background} "+"]]))

(defn group-view []
  [:div.group
   (channels)
   [content-view]
   [:div.edge-wrap
    (members)]])

(defn login []
  (let [value (atom "")
        change-handler (fn [e] (reset! value (-> e .-target .-value)))
        submit-handler (fn [e] (do
                                 (.preventDefault e)
                                 (events/login @value)))]
    [:div.modal.top-to-bottom
     [:form.add-group-wrap {:on-submit submit-handler}
      [:span.modal-title "Choose Username"]
      [:input.add-group-name {:on-change change-handler :placeholder "Username"}]
      [:input.btn-big {:type "submit"}]]]))

(defn main-panel []
  (let [login-needed? (re-frame/subscribe [:login-needed?])
        group-selected? @(re-frame/subscribe [:group-selected?])]
    (if @login-needed?
      (login)
      [:div.main.flex-row.overflow-hidden
       (groups-view)
       (if group-selected? (group-view))
       (background-dim)
       (add-group)])))
