(ns clj-chat.core
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [clj-chat.events]
              [clj-chat.subs]
              [clj-chat.views :as views]
              [clj-chat.config :as config]
              [clj-chat.socket :as socket]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
