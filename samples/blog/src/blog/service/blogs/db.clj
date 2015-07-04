(ns blog.service.blogs.db
  (:require [datomic.api :as d]))

(defn hydrate-blog [blog-eid db]
  (d/pull db '[:blog/id :blog/title :blog/content :blog/published-at] blog-eid))

(defn get-published-blogs [db]
  (->> (d/q '{:find [?blog-eid]
              :where [[?blog-eid :blog/published-at]]}
            db)
       (map (comp #(hydrate-blog % db) first))))

(defn get-published-blog-by-id [db blog-id]
  (-> (d/q '{:find [?blog-eid]
             :in [$ ?blog-id]
             :where [[?blog-eid :blog/published-at]
                     [?blog-eid :blog/id ?blog-id]]}
           db
           blog-id)
      ffirst
      (hydrate-blog db)))

(comment
  (d/transact (:datomic-conn user/system)
              [{:db/id #db/id [:db.part/user]
                :blog/id #uuid "32928fa7-2301-4f8f-9441-9908d42e24cd"
                :blog/title "Test blog"
                :blog/content "Test content"
                :blog/published-at (java.util.Date.)}])

  (get-published-blogs (d/db (:datomic-conn user/system)))

  (get-published-blog-by-id (d/db (:datomic-conn user/system))
                            #uuid "32928fa7-2301-4f8f-9441-9908d42e24cd"))
