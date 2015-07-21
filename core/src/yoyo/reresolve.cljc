(ns yoyo.reresolve)

(defn with-reresolve [v]
  (cond
    (var? v) (with-reresolve (symbol (str (ns-name (:ns (meta v))))
                                     (str (:name (meta v)))))

    (symbol? v) (let [v-ns (symbol (namespace v))
                      v-name (symbol (name v))]
                  (fn [& args]
                    (apply #?(:clj
                              (do
                                (require v-ns)
                                (ns-resolve (find-ns v-ns)
                                            v-name))

                              :cljs
                              v)

                           args)))

    :else v))
