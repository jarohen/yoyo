(ns yoyo.cljs
  (:require [yoyo.cljs.file-watcher :as watch]
            [bidi.ring :as br]
            [clojure.core.async :as a :refer [go-loop]]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]))

(defprotocol CLJSCompiler
  (bidi-routes [_])
  (cljs-handler [_])
  (path-for-js [_])
  (path-for-module [_ module]))

(try
  (require '[cljs.closure :as cljs]
           '[cljs.env :as cljs-env])

  (catch Exception e))

(defmacro assert-cljs [& body]
  (if (find-ns 'cljs.closure)
    `(do ~@body)
    `(throw (Exception. "No CLJS dependency available."))))

(defn- normalise-output-locations [{:keys [web-context-path target-path classpath-prefix modules], :or {target-path "target/cljs"} :as opts} build-mode]
  (let [output-dir (doto (io/file target-path (name build-mode))
                     .mkdirs)
        mains-dir (doto (io/file output-dir "mains" (or classpath-prefix ""))
                    .mkdirs)
        module-dir (when (not-empty modules)
                     (doto (io/file mains-dir "modules")
                       .mkdirs))]
    (-> opts
        (cond-> (empty? modules) (assoc :output-to (.getPath (io/file mains-dir "main.js"))))

        (assoc :output-dir (.getPath output-dir)
               :target-path target-path
               :asset-path web-context-path)

        (update-in [:modules] (fn [modules]
                                (when modules
                                  (->> (for [[module-key module-opts] modules]
                                         [module-key (assoc module-opts
                                                       :output-to (.getPath (io/file module-dir
                                                                                     (str (name module-key) ".js"))))])
                                       (into {}))))))))

(defn- compile-cljs! [{:keys [source-paths target-path] :as cljs-opts} cljs-compiler-env]
  (assert-cljs
   (assert (not-empty source-paths) "Please provide some source-paths!")

   (let [start-time (System/nanoTime)
         cljs-compilable (reify cljs/Compilable
                           (-compile [_ opts]
                             (mapcat #(cljs/-compile % opts) source-paths)))]

     (log/infof "Compiling CLJS, from %s to '%s'..." source-paths target-path)

     (try
       (log/with-logs ['cljs.closure :debug :warn]
         (cljs/build cljs-compilable cljs-opts cljs-compiler-env))

       (log/infof "Compiled CLJS, from %s to '%s', in %.2fs."
                  source-paths
                  target-path
                  (/ (- (System/nanoTime) start-time) 1e9))
       (catch Exception e
         (log/errorf e "Error compiling CLJS..."))))))

(defn build-cljs! [cljs-opts]
  (assert-cljs
   (let [{:keys [output-dir] :as cljs-opts} (-> cljs-opts
                                                (merge (:build cljs-opts))
                                                (normalise-output-locations :build))
         cljs-compiler-env (cljs-env/default-compiler-env cljs-opts)]

     (compile-cljs! cljs-opts cljs-compiler-env)

     (.getPath (io/file output-dir "mains")))))

(defn watch-cljs! [{:keys [source-paths] :as cljs-opts} latch-ch]
  (assert-cljs
   (let [{file-change-ch :out-ch, file-watch-latch-ch :latch-ch} (watch/watch-files! source-paths)

         {:keys [target-path web-context-path], :as cljs-opts} (-> cljs-opts
                                                                   (merge (:dev cljs-opts))
                                                                   (normalise-output-locations :dev))
         cljs-compiler-env (cljs-env/default-compiler-env cljs-opts)]

     (compile-cljs! cljs-opts cljs-compiler-env)

     (log/infof "Watching CLJS directories %s..." source-paths)

     (go-loop []
       (a/alt!
         file-change-ch (do
                          (compile-cljs! cljs-opts cljs-compiler-env)
                          (recur))

         latch-ch (do
                    (log/infof "Stopped watching CLJS directories %s." source-paths)
                    (a/close! file-watch-latch-ch))))

     (reify CLJSCompiler
       (bidi-routes [_]
         [web-context-path (br/files {:dir (.getPath (io/file target-path (name :dev)))})])

       (cljs-handler [this]
         (br/make-handler (bidi-routes this)))

       (path-for-js [_]
         (format "%s/mains/main.js" web-context-path))

       (path-for-module [_ module]
         (format "%s/mains/modules/%s.js" web-context-path (name module)))))))

(defn pre-built-cljs-compiler [{:keys [web-context-path] :as cljs-opts}]
  (log/info "Using pre-built CLJS")

  (reify CLJSCompiler
    (bidi-routes [_]
      [web-context-path (br/resources {:prefix (get-in cljs-opts [:build :classpath-prefix])})])

    (cljs-handler [this]
      (br/make-handler (bidi-routes this)))

    (path-for-js [_]
      (format "%s/main.js" web-context-path))

    (path-for-module [_ module]
      (format "%s/modules/%s.js" web-context-path (name module)))))

(defn pre-built? [cljs-opts]
  (let [classpath-prefix (get-in cljs-opts [:build :classpath-prefix])]
    (boolean (or (io/resource (str classpath-prefix "/modules"))
                 (io/resource (str classpath-prefix "/main.js"))))))

(defn with-cljs-compiler [{:keys [cljs-opts]} f]
  (if-not (pre-built? cljs-opts)
    (let [latch-ch (a/chan)]
      (try
        (f (watch-cljs! cljs-opts latch-ch))

        (finally
          (a/close! latch-ch))))

    (f (pre-built-cljs-compiler cljs-opts))))
