(ns mixer.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defonce app-state (atom {:count 0 :song (js/Howl. #js {:src #js ["full_mix.mp3"]})}))

(defn mutate [{:keys [state] :as env} key params]
  (if (= 'increment key)
    {:value {:keys [:count]}
     :action #(swap! state update-in [:count] inc)}
    {:value :not-found}))

(defn read [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [value (get st key)]
      {:value value}
      {:value :not-found})))

(defui Counter
  static om/IQuery
  (query [this]
         [:count])
  Object
  (render [this]
          (let [{:keys [count song]} (om/props this)]
            (dom/div nil
                     (dom/span nil (str "Count: " count))
                     (dom/button
                      #js {:onClick
                           (fn [e] (om/transact! this '[(increment)]))}
                      "Click me!")
                     (dom/div nil (str "all props" (om/props this)))
                     (dom/button
                      #js {:onClick
                           (fn [e] (.play (:song @app-state)))}
                      "Play")
                     (dom/button
                      #js {:onClick
                           (fn [e] (.pause (:song @app-state)))}
                      "Pause")))))

(def reconciler
  (om/reconciler
   {:state app-state
    :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler
              Counter (gdom/getElement "app"))
