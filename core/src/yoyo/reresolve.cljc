(ns yoyo.reresolve)

(defn with-reresolve [v]
  (cond
    (var? v) #?(:clj
                (with-reresolve (symbol (str (ns-name (:ns (meta v))))
                                        (str (:name (meta v)))))

                :cljs v)

    #?@(:clj [(symbol? v) (let [v-ns (symbol (namespace v))
                                v-name (symbol (name v))]
                            (fn [& args]
                              (require v-ns)

                              (apply (or (ns-resolve (find-ns v-ns)
                                                     v-name)
                                         (println "uh oh!")
                                         (throw (ex-info "Can't resolve system-fn!"
                                                         {:sym v})))
                                     args)))])

    :else v))
