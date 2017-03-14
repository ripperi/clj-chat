(ns clj-chat.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [resource-response content-type]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.defaults]
            [taoensso.sente :as sente]
            [datomic.api :as datomic]
            [org.httpkit.server :as http-kit]
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
  (ring.middleware.defaults/wrap-defaults
   ring-routes ring.middleware.defaults/site-defaults))

(def dev-handler (-> #'ring-handler wrap-reload))

;; ----------atoms to keep track of rooms and users----------

(defonce rooms_ (atom {}))

(defonce users_ (atom {}))

;; ----------functions for managing said atoms----------

(defn add-room
  ([rooms name]
   (add-room rooms name nil))
  ([rooms name creator-id]
   (assoc rooms (keyword name) {:id name :name name :users [] :channels ["#general"] :owner creator-id})))

(defn add-user
  ([users id]
   (add-user users id nil []))
  ([users id name room-keys]
   (assoc users (keyword id) {:id id :name name :rooms room-keys})))

(defn add-to-room! [rooms users room user]
  (swap! rooms update-in [(keyword room) :users] conj (keyword user))
  (swap! users update-in [(keyword user) :rooms] conj room))

(defn remove-from-room! [rooms users room user]
  (swap! rooms update-in [(keyword room) :users] #(vec (remove #{(keyword user)} %)))
  (swap! users update-in [(keyword user) :rooms] #(vec (remove #{room} %))))

(defn remove-user! [rooms users user]
  (doseq [room (:rooms ((keyword user) @users))]
    (remove-from-room! rooms users room user))
  (swap! users dissoc (keyword user)))

(defn get-rooms-with-keys [rooms users user]
  (select-keys rooms ((keyword user) users)))

(defn get-rooms [rooms users user]
  (mapv #((keyword %) rooms) (:rooms ((keyword user) users))))

;; ----------sente event handlers----------

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defmethod -event-msg-handler
  :default
  [{:keys [id ?data uid]}]
  (println "unmatched event")
  (chsk-send! uid [::unmatched-event {:id id :?data ?data}]))

;; -----

(defmethod -event-msg-handler :chsk/ws-ping [msg])

(defmethod -event-msg-handler
  :chsk/uidport-close
  [{:keys [uid]}]
  (remove-user! rooms_ users_ uid)
  (println (str "\nuidport-close\n" @users_ "\n" @rooms_ "\n")))

(defmethod -event-msg-handler
  :chsk/uidport-open
  [{:keys [uid]}]
  (reset! users_ (add-user @users_ uid))
  (add-to-room! rooms_ users_ "p u blic" uid)
  (update-rooms @rooms_ @users_ uid)
  (println (str "\nuidport-open\n" @users_ "\n" @rooms_ "\n")))

;; -----

(defmethod -event-msg-handler
  :clj-chat.events/message
  [{:keys [?data]}]
  (doseq [uuid (:any @connected-uids)]
    (chsk-send! uuid [::message ?data])))

(defmethod -event-msg-handler
  :room/add
  [{:keys [uid ?data]}]
  (if-not (map? ((keyword ?data) @rooms_))
    (do (reset! rooms_ (add-room @rooms_ ?data uid))
        (add-to-room! rooms_ users_ ?data uid)))
  (update-rooms @rooms_ @users_ uid)
  (println (str "\nadd room\n" @rooms_ "\n" @users_ "\n")))

;; ----------sente send events----------

(defn update-rooms [rooms users user]
  (chsk-send! user [:update/rooms (get-rooms rooms users user)]))

;; ---------- sente router ----------

(defonce router_ (atom nil))

(defn stop-router! []
  (reset! rooms_ {})
  (reset! users_ {})
  (when-let [stop-fn @router_] (stop-fn)))

(defn start-router! []
  (stop-router!)
  (reset! rooms_ (add-room @rooms_ "p u blic"))
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk -event-msg-handler)))
