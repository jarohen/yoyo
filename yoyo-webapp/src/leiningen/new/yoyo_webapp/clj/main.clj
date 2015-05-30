(ns {{name}}.service.main
  (:gen-class))

(defn -main []
  (require 'yoyo
           '[nrepl.embed :as nrepl])

  (eval '(do
           (nrepl/start-nrepl! {:port 7888})

           (yoyo/set-system-fn! '{{name}}.service.system/make-system)

           (yoyo/start!))))
