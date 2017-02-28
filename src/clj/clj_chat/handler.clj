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

(defroutes ring-routes
  (GET "/" [] (content-type (resource-response "index.html" {:root "public"}) "text/html"))
  (GET  "/chsk" req (ring-ajax-get-or-ws-handshake req))
  (POST "/chsk" req (ring-ajax-post                req))
  (resources "/"))

(def ring-handler
  (ring.middleware.defaults/wrap-defaults
   ring-routes ring.middleware.defaults/site-defaults))

(def dev-handler (-> #'ring-handler wrap-reload))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defmethod -event-msg-handler
  :default
  [{:keys [id ?data]}]
  (println "SERVER DEFAULT HANDLER")
  (println "-id:" id "-data:" ?data)
  (chsk-send! :sente/all-users-without-uid [::unmatched-event {:id id :?data ?data}]))

(defmethod -event-msg-handler :chsk/ws-ping [msg])

(defmethod -event-msg-handler :chsk/uidport-close [msg])

(defmethod -event-msg-handler :chsk/uidport-open [msg])

(defmethod -event-msg-handler
  :clj-chat.events/message
  [{:as msg :keys [id ?data]}]
  (println "SERVER MESSAGE HANDLER")
  (println "-id:" id "-data:" ?data)
  (chsk-send! :sente/all-users-without-uid [::message ?data]))

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk -event-msg-handler)))
