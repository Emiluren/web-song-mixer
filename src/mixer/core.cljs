(ns mixer.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(defui HelloWorld
  Object
  (render [this]
          (dom/div nil (get (om/props this) :title))))

(def hello (om/factory HelloWorld))

(js/ReactDOM.render
 (apply dom/div nil
        (map #(hello {:react-key %
                      :title (str "Hello " %)})
             ["John", "bruh", "Daisy", "and goodbye"]))
 (gdom/getElement "app"))
