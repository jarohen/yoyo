(ns yoyo.dep.protocols)

;; Dependent a = Resolved a | Nested Key (System -> Dependent a) | Env (System -> Dependent a)

(defprotocol Dependent
  (dbind [_ f])
  (try-satisfy [_ system]))

(defn dependent? [dependent]
  (and dependent
       (satisfies? Dependent dependent)))

(defrecord ResolvedDependent [v]
  Dependent
  (dbind [_ f]
    (f v))

  (try-satisfy [_ system]
    v))

(defrecord NestedDependent [dep-key f]
  Dependent
  (dbind [_ inner-f]
    (->NestedDependent dep-key
                       (fn [system]
                         ;; (f system) :: Dependent a
                         ;; (dbind (f system) inner-f) :: Dependent b
                         (dbind (f system) inner-f))))

  (try-satisfy [this system]
    (if (contains? system dep-key)
      (f system)
      this)))

(defrecord EnvDependent [f]
  Dependent
  (dbind [_ inner-f]
    (->EnvDependent (fn [system]
                      (dbind (f system) inner-f))))

  (try-satisfy [this system]
    (try-satisfy (f system) system)))

(defn env-dependent []
  (->EnvDependent (fn [system]
                    (->ResolvedDependent (:env (meta system))))))
