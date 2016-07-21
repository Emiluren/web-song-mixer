(ns mixer.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [ajax.core :refer [GET]]
            [sablono.core :as html :refer-macros [html]]))

(defn load-track [track]
  (let [name (get track "name")
        files (map #(str "songs/" %) (get track "files"))]
    {:name name :audio (js/Howl. #js {:src (into-array files)})}))

(defonce app-state (atom {}))

(defn songs-handler [songs]
  (swap! app-state assoc :songs songs))

(GET "songs.json" {:handler songs-handler})

(defn on-click-play [e]
  (doseq [track (:loaded-tracks @app-state)]
    (.play (:audio track)))
  #_(swap! app-state assoc :playid
         (.play (:song @app-state))))

(defn on-click-pause [e]
  (doseq [track (:loaded-tracks @app-state)]
    (.pause (:audio track))))

(defn on-click-stop [e]
  (doseq [track (:loaded-tracks @app-state)]
    (.stop (:audio track))))

(defn on-song-click [song]
  (on-click-stop nil)
  (let [tracks (get song "tracks")]
    (swap! app-state assoc
           :loaded-song song
           :loaded-tracks (map load-track tracks))))

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
  [:div [:span {:style {:display "inline-block" :width "100px" :font-weight "bold"}}
         (get track "name")]
   [:label "Volym: "
    [:input {:type "range" :min 0 :max 100 :default-value 100}]]
   [:label " Balans: "
    [:input {:type "range" :min -100 :max 100 :default-value 0}]]])

(defn create-song-ui [song]
  (if (every? #(= "loaded" (.state (:audio %))) (get :loaded-tracks @app-state))
    [:div
     [:div
      [:button {:onClick on-click-play} "Play"]
      [:button {:onClick on-click-pause} "Pause"]
      [:button {:onClick on-click-stop} "Stop"]]

     (into [:div [:h3 (get song "title")]]
           (map create-track-ui (get song "tracks")))]
    "Ljudfiler laddas in"))

(defn create-song-list [songs]
  (map
   (fn [song]
     [:li {:key (get song "title")}
      [:a
           {:onClick (fn [e] (on-song-click song)) :href "#"}
           (get song "title")]])
   songs))

(defn generated-html [component]
  (let [{:keys [loaded-song songs]} (om/props component)]
    (html [:div
           (if (= :not-found songs)
             "Laddar ner låtlista"
             [:ul (create-song-list songs)])

           (if (= :not-found loaded-song)
             "Klicka på en låt i listan"
             (create-song-ui loaded-song))])))

(defui Mixer
  static om/IQuery
  (query [this]
         [:loaded-song :songs])
  Object
  (render [this]
            (generated-html this)))

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler
              Mixer (gdom/getElement "app"))
