(ns mixer.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [ajax.core :refer [GET]]
            [sablono.core :as html :refer-macros [html]]))

(defn fractions [values]
  (let [total (count values)
        kval-to-kfrac (fn [[k v]]
                        [k (* (/ 1 total) v)])]
    (into {} (map kval-to-kfrac (frequencies values)))))

(defn show-load-error [files name id msg]
  (js/alert (str "Kunde inte läsa in filerna "
                 files " för spåret " name "("
                 id ", " msg ")")))

(defonce app-state (atom {}))

(defn load-track [track]
  (let [name (get track "name")
        files (map #(str "songs/" %) (get track "files"))
        options #js {:src (into-array files)
                     :onloaderror #(show-load-error files name %1 %2)
                     :onload #(swap! app-state assoc-in
                                     [:finished-loading name] true)}]
    {:name name :audio (js/Howl. options)}))


(defn songs-handler [songs]
  (swap! app-state assoc :songs songs))

(GET "songs.json" {:handler songs-handler})

(defn play-tracks! []
  (doseq [track (:loaded-tracks @app-state)]
    (.play (:audio track))))

(defn pause-tracks! []
  (doseq [track (:loaded-tracks @app-state)]
    (.pause (:audio track))))

(defn stop-tracks! []
  (doseq [track (:loaded-tracks @app-state)]
    (.stop (:audio track))))

(defn loaded-false [tracks]
  (let [false-vec (fn [name] [name false])
        track-names (map #(get % "name") tracks)]
    (into {} (map false-vec track-names))))

(defn reset-finished-loading! [tracks]
  (swap! app-state assoc :finished-loading (loaded-false tracks)))

(defn load-new-song! [song]
  (stop-tracks!)
  (let [tracks (get song "tracks")]
    (reset-finished-loading! tracks)
    (swap! app-state assoc
           :selected-song song
           :loaded-tracks (doall (map load-track tracks)))))

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

(defn create-song-ui [song finished-loading]
  (if (every? identity (vals finished-loading))
    [:div
     [:div
      [:button {:onClick play-tracks!} "Play"]
      [:button {:onClick pause-tracks!} "Pause"]
      [:button {:onClick stop-tracks!} "Stop"]]

     (into [:div [:h3 (get song "title")]]
           (map create-track-ui (get song "tracks")))]
    (str "Ljudfiler laddas in " (-> (fractions (vals finished-loading))
                                    (get true)
                                    (* 100)
                                    int)
         "%.")))

(defn create-song-list [songs]
  (map
   (fn [song]
     [:li {:key (get song "title")}
      [:a
           {:onClick (fn [e] (load-new-song! song)) :href "#"}
           (get song "title")]])
   songs))

(defn generated-html [component]
  (let [{:keys [selected-song songs finished-loading]} (om/props component)]
    (html [:div
           #_(str finished-loading)
           (if (= :not-found songs)
             "Laddar ner låtlista"
             [:ul (create-song-list songs)])

           (if (= :not-found selected-song)
             "Klicka på en låt i listan"
             (create-song-ui selected-song finished-loading))])))

(defui Mixer
  static om/IQuery
  (query [this]
         [:selected-song :songs :finished-loading])
  Object
  (render [this]
            (generated-html this)))

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler
              Mixer (gdom/getElement "app"))
