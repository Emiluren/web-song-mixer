(ns mixer.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(def app-state (atom {:count 0 :extra " yo"}))

(defui Counter
  Object
  (render [this]
    (let [{:keys [count extra]} (om/props this)]
      (dom/div nil
        (dom/span nil (str "Count: " count extra))
        (dom/br nil)
        (dom/button
          #js {:onClick
               (fn [e]
                 (swap! app-state update-in [:count] inc))}
          "Click me!")))))

(def reconciler
  (om/reconciler {:state app-state}))

(om/add-root! reconciler
  Counter (gdom/getElement "app"))
