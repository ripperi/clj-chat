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

(defonce rooms_ (atom {}))
(swap! rooms_ assoc :public {:name "Public" :users [] :channels []})

(defonce users_ (atom {}))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defmethod -event-msg-handler
  :default
  [{:keys [id ?data uid]}]
  (println "unmatched event")
  (chsk-send! uid [::unmatched-event {:id id :?data ?data}]))

(defmethod -event-msg-handler :chsk/ws-ping [msg])

(defmethod -event-msg-handler :chsk/uidport-close [msg]
  (swap! rooms_ update-in [:public :users] #(vec (remove #{(:uid msg)} %)))
  (println @rooms_))

(defmethod -event-msg-handler :chsk/uidport-open [msg]
  (if-not (= (:uid msg) :taoensso.sente/nil-uid)
    (swap! rooms_ update-in [:public :users] conj (:uid msg)))
  (println @rooms_))

(defmethod -event-msg-handler
  :clj-chat.events/message
  [{:keys [?data]}]
  (doseq [uuid (:any @connected-uids)]
    (chsk-send! uuid [::message ?data])))

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk -event-msg-handler)))
