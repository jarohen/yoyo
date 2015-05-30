(ns {{name}}.ui.app
  (:require [clojure.string :as s]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(enable-console-print!)

(defn hello-world []
  (om/component
   (html [:p "Hello world!"])))

(set! (.-onload js/window)
      (fn []
        (om/root hello-world {} {:target js/document.body})))

;; ------------------------------------------------------------

;; Below this line is only required for the Yo-yo welcome page, feel
;; free to just delete all of it when you want to get cracking on your
;; own project!

(defn code [s]
  [:strong {:style {:font-family "'Courier New', 'monospace'"}}
   s])

(defn demo-component []
  (om/component
   (html
    [:div.container
     [:h2 {:style {:margin-top "1em"}}
      "Hello from Yo-yo!"]

     [:h3 "Things to try:"]

     [:ul
      [:li [:p "In your Clojure REPL, run " (code "(yoyo/reload!)") " to completely reload the webapp without restarting the JVM."]]
      [:li [:p "Start making your webapp!"]
       [:ul
        [:li [:p "The CLJS entry point is in " (code "ui-src/{{sanitized}}/ui/app.cljs")]]
        [:li [:p "The Clojure Ring handler is in " (code "src/{{sanitized}}/service/handler.clj")]]]]

      [:li [:p "Any trouble, let me know - either through GitHub or on Twitter at " [:a {:href "https://twitter.com/jarohen"} "@jarohen"]]]

      [:li [:p "Good luck!"]]]

     [:div {:style {:text-align "right"
                    :font-weight "bold"}}
      [:p
       [:span {:style {:font-size "1.3em"}} "James Henderson"]
       [:br]
       "Twitter: " [:a {:href "https://twitter.com/jarohen"} "@jarohen"]
       [:br]
       "GitHub: " [:a {:href "https://github.com/james-henderson"} "james-henderson"]]]])))

(set! (.-onload js/window)
      (fn []
        (om/root demo-component {} {:target js/document.body})))
