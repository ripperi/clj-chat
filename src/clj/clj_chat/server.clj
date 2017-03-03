(ns clj-chat.server
  (:require [clj-chat.handler :refer [ring-handler dev-handler start-router! stop-router!]]
            [config.core :refer [env]]
            [org.httpkit.server :as kit])
  (:gen-class))

(defonce web-server_ (atom nil))
(defn stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))
(defn start-web-server! []
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (stop-web-server!)
    (reset! web-server_ (kit/run-server #'dev-handler {:port port}))))

(defn stop! [] (stop-router!) (stop-web-server!))
(defn start! [] (start-router!) (start-web-server!))

(defn -main [& args] (start!))

