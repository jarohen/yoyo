(ns {{name}}.ui.app
  (:require [clojure.string :as s]
            [reagent.core :as r]
            [nrepl.embed :refer [connect-brepl!]]))

(enable-console-print!)

(connect-brepl!)

(defn hello-world []
  [:p "Hello world!"])

(set! (.-onload js/window)
      (fn []
        (r/render-component [hello-world]
                            js/document.body)))

;; ------------------------------------------------------------

;; Below this line is only required for the Yo-yo welcome page, feel
;; free to just delete all of it when you want to get cracking on your
;; own project!

(defn code [s]
  [:strong {:style {:font-family "'Courier New', 'monospace'"}}
   s])

(defn demo-component []
  [:div.container
   [:h2 {:style {:margin-top "1em"}}
    "Hello from Yo-yo!"]

   [:h3 "Things to try:"]

   [:ul
    [:li [:p "In your Clojure REPL, run " [code "(yoyo/reload!)"] " to completely reload the webapp without restarting the JVM."]]
    [:li [:p "Start making your webapp!"]
     [:ul
      [:li [:p "The CLJS entry point is in " [code "ui-src/{{sanitized}}/ui/app.cljs"]]]
      [:li [:p "The Clojure system entry point is in " [code "src/{{sanitized}}/service/system.clj"]]]
      [:li [:p "The Clojure Ring handler is in " [code "src/{{sanitized}}/service/handler.clj"]]]]]

    [:li [:p "Connect to the CLJS browser REPL"]
     [:ol
      [:li "Connect to the normal server-side REPL (port 7888, by default)"]
      [:li "Evaluate: " [code "(nrepl.embed/->brepl)"]]
      [:li "Refresh this page"]
      [:li "When you get a " [code "cljs.user =>"] " prompt, you can test it with:"
       [:ul
        [:li [code "(+ 1 1)"]]
        [:li [code "(js/window.alert \"Hello world!\")"]]
        [:li [code "(set! (.-backgroundColor js/document.body.style) \"green\")"]]]]]]

    [:li [:p "Any trouble, let me know - either through GitHub or on Twitter at " [:a {:href "https://twitter.com/jarohen"} "@jarohen"]]]

    [:li [:p "Good luck!"]]]

   [:div {:style {:text-align "right"
                  :font-weight "bold"}}
    [:p
     [:span {:style {:font-size "1.3em"}} "James Henderson"]
     [:br]
     "Twitter: " [:a {:href "https://twitter.com/jarohen"} "@jarohen"]
     [:br]
     "GitHub: " [:a {:href "https://github.com/james-henderson"} "james-henderson"]]]])

(set! (.-onload js/window)
      (fn []
        (r/render-component [demo-component]
                            js/document.body)))
