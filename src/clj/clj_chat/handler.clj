(ns clj-chat.handler
  (:require [clj-chat.core :as core]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response content-type]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults]
            [taoensso.sente :as sente]
            [org.httpkit.server :as http-kit]
            [config.core :refer [env]]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {})]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defn root-handler
  [req]
  (let [{:keys [session params]} req
        {:keys [user-id]} params]
    (assoc
     (content-type (resource-response "index.html" {:root "public"}) "text/html")
     :session (assoc session :uid (str (java.util.UUID/randomUUID))))))

(defroutes ring-routes
  (GET "/" req (root-handler req))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (resources "/"))

(def ring-handler
  (-> #'ring-routes
      (cond-> (env :port) wrap-reload)
      (ring.middleware.defaults/wrap-defaults ring.middleware.defaults/site-defaults)))

;; ----------sente send events----------

(defn update-clients-rooms [user]
  (chsk-send! user [:update/rooms (core/get-rooms user)]))

(defn update-neighbouring-users-rooms [user]
  (doseq [nb (core/get-neighbouring-users user)]
    (update-clients-rooms nb)))

(defn send-login-need-status [uid bool]
  (chsk-send! uid [:update/login-need bool]))

;; ----------sente event handlers----------

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defmethod -event-msg-handler
  :default
  [{:keys [id ?data uid]}]
  #_(println "unmatched event")
  (chsk-send! uid [::unmatched-event {:id id :?data ?data}]))

;; -----

(defmethod -event-msg-handler :chsk/ws-ping [msg])

(defmethod -event-msg-handler
  :chsk/uidport-close
  [{:keys [uid]}]
  (core/remove-user-from-rooms! uid)
  (update-neighbouring-users-rooms uid)
  (core/remove-user-from-users! uid)
  #_(println (str "\nuidport-close\n" @core/users_ "\n\n" @core/rooms_ "\n")))

(defmethod -event-msg-handler
  :chsk/uidport-open
  [{:keys [uid]}]
  (core/add-user! uid)
  #_(println (str "\nuidport-open\n" @core/users_ "\n\n" @core/rooms_ "\n")))

;; -----

(defmethod -event-msg-handler
  :clj-chat.events/message
  [{:keys [uid ?data]}]
  (if (and (core/valid-message? ?data) (core/allowed-message? uid ?data))
    (doseq [uuid (core/get-users-in-room (:group ?data))]
      (chsk-send! uuid [::message (assoc ?data :from (select-keys (core/get-user uid) [:id :name]))]))))

(defmethod -event-msg-handler
  :clj-chat.events/direct-message
  [{:keys [uid ?data]}]
  (let [msg [::direct-message (assoc ?data :from (select-keys (core/get-user uid) [:id :name]))]
        to-uid (get-in ?data [:to :id])]
    (when-not (= uid to-uid)
        (chsk-send! to-uid msg))
    (chsk-send! uid msg)))

(defmethod -event-msg-handler
  :room/add
  [{:keys [uid ?data]}]
  (if (core/room-name-free? ?data)
    (do (core/add-room! ?data uid)
        (core/add-to-room! ?data uid)
        (update-clients-rooms uid)))
  #_(println (str "\nadd room\n" @core/rooms_ "\n" @core/users_ "\n")))

(defmethod -event-msg-handler
  :add/channel
  [{:keys [uid ?data]}]
  (let [room (:group ?data)
        channel (:channel ?data)]
    (if (and (core/room-owner? room uid)
             (core/channel-name-free? room channel))
      (do (core/add-channel! room channel)
          (update-neighbouring-users-rooms uid)
          (update-clients-rooms uid))))
  #_(println (str "\nadd channel\n" @core/rooms_ "\n" @core/users_ "\n")))

(defmethod -event-msg-handler
  :update/login
  [{:keys [uid ?data]}]
  (if (core/login-needed? uid)
    (do (core/add-to-room! "public" uid)
        (core/set-username! uid ?data)
        (update-clients-rooms uid)
        (update-neighbouring-users-rooms uid)
        (send-login-need-status uid false)
        #_(println (str "\nLOGIN\n" @core/users_ "\n\n" @core/rooms_)))))

;; ---------- sente router ----------

(defonce router_ (atom nil))

(defn stop-router! []
  (reset! core/rooms_ {})
  (reset! core/users_ {})
  (when-let [stop-fn @router_] (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! core/rooms_ (core/add-room! "public"))
  (core/add-channel! :public "#general2")
  (core/add-channel! :public "#general3")
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk -event-msg-handler)))
