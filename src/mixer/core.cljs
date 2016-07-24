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
                     :html5 true ;; Needed for changing rate independent of pitch
                     :onloaderror #(show-load-error files name %1 %2)
                     :onload #(swap! app-state assoc-in
                                     [:finished-loading name] true)}]
    {:name name :audio (js/Howl. options) :muted false}))


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

(defn index-of-first [pred coll]
  (first
   (keep-indexed (fn [idx x]
                   (when (pred x)
                     idx))
                 coll)))

(defn track-index [track-name tracks]
  (index-of-first #(= (:name %) track-name) tracks))

;; Does not work
;; Assert failed: mixer.core/toggle-mute mutation
;; :value must be nil or a map with structure {:keys [...]}
;; (or (nil? value) (map? value))
(defn mutate [{:keys [state] :as env} key {:keys [track-name] :as params}]
  (if (= 'toggle-mute key)
    (let [idx (track-index track-name (:loaded-tracks state))]
      {:value {:keys [:loaded-tracks]}
       :action #(swap! state update-in [:loaded-tracks idx :muted] not)})
    {:value :not-found}))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [value (get st key)]
      {:value value}
      {:value :not-found})))

(defn audio-for-track-name [name]
  (some #(when (= (:name %) name)
           (:audio %))
        (:loaded-tracks @app-state)))

(defn target-value [event]
  (.-value (.-target event)))

(defn set-volume! [name value]
  (let [audio (audio-for-track-name name)]
    (.volume audio (/ value 100))))

(defn set-panning! [name value]
  (let [audio (audio-for-track-name name)]
    (.stereo audio (/ value 100))))

(defn toggle-mute! [name]
  (let [audio (audio-for-track-name name)]
    (.mute audio (not (.mute audio)))))

(defn set-rate! [value]
  (doseq [track (:loaded-tracks @app-state)]
    (.rate (:audio track) (/ value 100))))

(defn create-slider [label min max default on-input]
  [:label label
   [:input {:type "range" :min min :max max :default-value default
            :on-input #(on-input (target-value %))}]])

(defn create-track-ui [component track]
  (let [name (get track "name")]
    [:div [:span {:style {:display "inline-block" :width "100px" :font-weight "bold"}}
           name]

     [:span {:style {:background-color
                     (if (.mute (audio-for-track-name name)) "#f00" "#fff")}
             :on-click #(toggle-mute! name)}
      "Mute"]

     [:span {:style {:background-color "#ff0"}} " Solo"]
     (create-slider " Volym: " 0 100 100 #(set-volume! name %))
     (create-slider " Balans: " -100 100 0 #(set-panning! name %))]))

(defn create-song-ui [component song finished-loading]
  (if (every? identity (vals finished-loading))
    [:div
     [:div
      [:button {:onClick play-tracks!} "Play"]
      [:button {:onClick pause-tracks!} "Pause"]
      [:button {:onClick stop-tracks!} "Stop"]

      (create-slider " Volym: " 0 100 100 #(.volume js/Howler (/ % 100)))
      (create-slider " Tempo (50% - 400%): " 50 400 100 set-rate!)]

     (into [:div [:h3 (get song "title")]]
           (map #(create-track-ui component %) (get song "tracks")))]
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
             (create-song-ui component selected-song finished-loading))])))

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
