(ns foli.handlers
  (:require
    [ajax.core :as a]
    [clojure.string :refer [join]]
    [cljs-time.core :as t]
    [cljs-time.coerce :as c]
    [cljs-time.format :as f]
    [re-frame.core :refer [register-handler dispatch debug]]))

(def foli-url "http://data.foli.fi/")

(defn- format-response [response]
  (map (fn [item]
     (let [estimated-time (t/to-default-time-zone (c/from-long (* 1000 (item "expectedarrivaltime"))))]
         {:display (item "destinationdisplay")
          :estimated-time estimated-time})) (response "result")))

(register-handler :fetch-stop-data
    (fn [app-state [_ stop-id]]
      (when (nil? (get-in app-state [:stops stop-id]))
        (a/GET (join "/" [ foli-url "siri/sm" stop-id]) {
                          :handler
                          (fn [result]
                            (dispatch [:set-stop-data stop-id result]))
                          :response-format :json}))
        app-state))

(register-handler :set-stop-data
    (fn [app-state [_ stop-id data]]
      (assoc-in app-state [:stops stop-id] (format-response data))))

(register-handler :fetch-stops
    (fn [app-state _]
      (when (nil? (:stop-names app-state))
          (a/GET (join "/" [foli-url "siri/sm"]) {
                :handler
                (fn [result]
                  (dispatch [:set-stops result]))
                :response-format :json}))
      app-state))

(register-handler :set-stops
    debug
    (fn [app-state [_ result]]
        (let [new-state  (-> app-state
            (assoc :stop-names (into {} (keep (fn [[k v]] [(v "stop_name") k])) result))
            (assoc :stop-ids (into {} (keep (fn [[k v]] [k (v "stop_name")])) result)))]
          new-state)))

(register-handler :set-selected-stop
    (fn [app-state [_ stop-id]]
      (dispatch [:fetch-stop-data stop-id])
      (assoc app-state :selected-stop stop-id)))

(register-handler :main
    (fn [app-state [_ stop-id]]
      (dissoc app-state :selected-stop)))

(register-handler :search-stop
    (fn [app-state [_ stop-name]]
      (when-not (nil? ((app-state :stop-ids) stop-name))
          (dispatch [:set-selected-stop stop-name]))
      app-state))

