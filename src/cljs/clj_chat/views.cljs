(ns clj-chat.views
  (:require [re-frame.core :as re-frame]
            [clj-chat.events :as events]
            [reagent.core :refer [atom create-class]]
            [cljs-time.format :as tf :refer [unparse formatter]]
            [cljs-time.coerce :as coerce :refer [from-long to-long to-string]]
            [cljs-time.core :as time :refer [to-default-time-zone]]
            [clojure.string :as str]))

(defn background-dim []
  (if @(re-frame/subscribe [:background-dim])
    [:div.background-dim {:on-click #(re-frame/dispatch [:toggle-modals-off])}]
    [:div.background-dim.hidden]))

(defn add-group []
  (if @(re-frame/subscribe [:add-group])
    (let [value (atom "")
          change-handler (fn [e] (reset! value (-> e .-target .-value)))
          submit-handler (fn [e] (do
                                   (.preventDefault e)
                                   (events/add-group @value)
                                   (re-frame/dispatch [:toggle-modal :add-group])))]
        [:div.modal.top-to-bottom
         [:form.add-group-wrap {:on-submit submit-handler}
          [:span.modal-title "Create Group"]
          [:input.add-group-name {:on-change change-handler :placeholder "Name"}]
          [:input.btn-big {:type "submit" :value "Create"}]]])))

(defn add-channel []
  (if @(re-frame/subscribe [:add-channel])
    (let [value (atom "")
          group @(re-frame/subscribe [:group-id])
          change-handler (fn [e] (reset! value (-> e .-target .-value)))
          submit-handler (fn [e] (do
                                   (.preventDefault e)
                                   (events/add-channel group @value)
                                   (re-frame/dispatch [:toggle-modals-off])))]
      [:div.modal.top-to-bottom
       [:form.add-group-wrap {:on-submit submit-handler}
        [:span.modal-title "Add Channel"]
        [:input.add-group-name {:on-change change-handler :placeholder "Name"}]
        [:input.btn-big {:type "submit" :value "Add"}]]])))

(defn add-member []
  (if @(re-frame/subscribe [:add-member])
    (let [value (atom "")
          group @(re-frame/subscribe [:group-id])
          change-handler (fn [e] (reset! value (-> e .-target .-value)))
          submit-handler (fn [e] (do
                                   (.preventDefault e)
                                   (events/add-member group @value)
                                   (re-frame/dispatch [:toggle-modals-off])))]
      [:div.modal.top-to-bottom
       [:form.add-group-wrap {:on-submit submit-handler}
        [:span.modal-title "Add Member"]
        [:input.add-group-name {:on-change change-handler :placeholder "Username"}]
        [:input.btn-big {:type "submit" :value "Add"}]]])))

(defn message-view [message]
  [:li.message {:key (:time message)}
   [:h5.message-header
    [:span.username (get-in message [:from :name])]
    [:span.timestamp (unparse (formatter "yyyy-MM-dd HH:mm") (to-default-time-zone (from-long (:time message))))]]
   [:span.msg (:value message)]])

(defn messages-view []
  (let [messages (re-frame/subscribe [:filtered-messages])
        scroll-bottom #(.scrollIntoView
                        (.getElementById js/document "dummy-elem")
                        #js {"behavior" "smooth"})]
    (create-class
     {:component-did-update scroll-bottom
      :component-did-mount scroll-bottom
      :reagent-render (fn [] [:ul.messages.scroller
                              (map message-view @messages)
                              [:div {:id "dummy-elem"}]])})))

(defn content-view []
  (let [value (atom "")
        show-content? (re-frame/subscribe [:show-content?])
        channel-name (re-frame/subscribe [:channel])
        member (re-frame/subscribe [:member])
        group-id (re-frame/subscribe [:group-id])
        change-handler (fn [e] (reset! value (-> e .-target .-value)))
        submit-handler (fn [e]
                         (.preventDefault e)
                         (if (and @channel-name (not @member))
                           (events/send-msg            {:time (str (.getTime (js/Date.)))
                                                        :value @value
                                                        :channel @channel-name
                                                        :group @group-id})
                           (events/send-direct-message {:time (str (.getTime (js/Date.)))
                                                        :value @value
                                                        :to @member}))
                         (reset! value ""))]
    (fn []
      (if @show-content?
        [:div.content.flex-col
         [messages-view]
         [:form.text-wrap {:on-submit submit-handler}
          [:div.text-wrap-inner.overflow-hidden
           [:input.text-area {:on-change change-handler :placeholder "Message..." :value @value}]
           [:input.text-area-btn {:type "submit" :value "send"}]]]]
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
  (let [group (re-frame/subscribe [:group])
        on-click #(re-frame/dispatch [:toggle-modal :add-channel])]
    [:div.edge-wrap
     [:header.header.clickable {:on-click on-click}
      [:div.header-title (:name @group)]
      [:span.add "+"]]
     [:div.separator]
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
  (let [members (re-frame/subscribe [:members])
        on-click #(re-frame/dispatch [:toggle-modal :add-member])]
    [:div
     [:header.header.clickable {:on-click on-click}
      [:div.header-title "Members"]
      [:span.add "+"]]
     [:div.separator]
     [:ul.channels (doall (map member @members))]]))

(defn groups-view []
  (let [toggle-modal #(re-frame/dispatch [:toggle-modal :add-group])
        groups (re-frame/subscribe [:groups])]
    [:div.groups
     [:ul.groups (doall (map select-group @groups))]
     [:div.add-group {:type "button" :on-click toggle-modal} "+"]]))

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
       (add-group)
       (add-channel)
       (add-member)])))
