(ns {{name}}.ui.app
  (:require [flow.core :as f :include-macros true]
            [clojure.string :as s]))

(enable-console-print!)

(set! (.-onload js/window)
      (fn []
        (f/root js/document.body
          (f/el
            [:p "Hello world!"]))))

;; ------------------------------------------------------------

;; Below this line is only required for the Yo-yo welcome page, feel
;; free to just delete all of it when you want to get cracking on your
;; own project!

(defn code [s]
  (f/el
    [:strong {::f/style {:font-family "'Courier New', 'monospace'"}}
     s]))

(set! (.-onload js/window)
      (fn []
        (f/root js/document.body
          (f/el
            [:div.container
             [:h2 {::f/style {:margin-top "1em"}}
              "Hello from Yo-yo!"]

             [:h3 "Things to try:"]

             [:ul
              [:li [:p "In your Clojure REPL, run " [code "(yoyo/reload!)"] " to completely reload the webapp without restarting the JVM."]]
              [:li [:p "Start making your webapp!"]
               [:ul
                [:li [:p "The CLJS entry point is in " [code "ui-src/{{sanitized}}/ui/app.cljs"]]]
                [:li [:p "The Clojure system entry point is in " [code "src/{{sanitized}}/service/system.clj"]]]
                [:li [:p "The Clojure Ring handler is in " [code "src/{{sanitized}}/service/handler.clj"]]]]]

              [:li [:p "Any trouble, let me know - either through GitHub or on Twitter at " [:a {:href "https://twitter.com/jarohen"} "@jarohen"]]]

              [:li [:p "Good luck!"]]]

             [:div {::f/style {:text-align "right"
                               :font-weight "bold"}}
              [:p
               [:span {::f/style {:font-size "1.3em"}} "James Henderson"]
               [:br]
               "Twitter: " [:a {:href "https://twitter.com/jarohen"} "@jarohen"]
               [:br]
               "GitHub: " [:a {:href "https://github.com/james-henderson"} "james-henderson"]]]]))))
