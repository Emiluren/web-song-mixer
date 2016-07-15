(ns mixer.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [ajax.core :refer [GET]]
            [sablono.core :as html :refer-macros [html]]))

(defonce app-state (atom {:current-song 0
                          :song (js/Howl. #js {:src #js ["full_mix.mp3"]})}))

(defn songs-handler [songs]
  (swap! app-state assoc :songs songs))

(GET "songs.json" {:handler songs-handler})

(defn on-click-play [e]
  (.play (:song @app-state)))

(defn on-click-pause [e]
  (.pause (:song @app-state)))

(defn mutate [{:keys [state] :as env} key params]
  (if (= 'increment key)
    {:value {:keys [:current-song]}
     :action #(swap! state update-in [:current-song] inc)}
    {:value :not-found}))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [value (get st key)]
      {:value value}
      {:value :not-found})))

(defn create-track-ui [track]
  [:div (str "files: " (get track "files"))])

(defn create-song-ui [song]
  (into [:div (get song "title")]
        (map create-track-ui (get song "tracks"))))

(defn generated-html [component]
  (let [{:keys [current-song songs]} (om/props component)]
    (html [:div
           [:span (str "Current-Song: " current-song)]
           [:button {:onClick (fn [e] (om/transact! component '[(increment)]))} "Click me!"]
           [:br]
           [:br]

           [:button {:onClick on-click-play} "Play"]
           [:button {:onClick on-click-pause} "Pause"]
           [:br]
           [:br]

           (into [:div] (map create-song-ui songs))])))

(defui Mixer
  static om/IQuery
  (query [this]
         [:current-song :songs])
  Object
  (render [this]
            (generated-html this)))

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler
              Mixer (gdom/getElement "app"))
