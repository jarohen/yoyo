(ns yoyo.resource.sink)

(defprotocol SystemSink
  (set-system! [_ running-system]))

(extend-protocol SystemSink
  #?(:clj clojure.lang.Atom
     :cljs cljs.core.Atom)
  (set-system! [!atom running-system]
    (reset! !atom running-system)))

#?(:clj
   (extend-protocol SystemSink
     clojure.lang.Symbol
     (set-system! [sym running-system]
       (let [the-ns (doto (symbol (namespace sym))
                      create-ns)
             the-name (symbol (name sym))]
         (intern the-ns the-name running-system)))))
