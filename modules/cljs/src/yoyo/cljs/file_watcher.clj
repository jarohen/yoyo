(ns yoyo.cljs.file-watcher
  (:require [clojure.core.async :as a :refer [go]]
            [clojure.java.io :as io])
  (:import [com.sun.nio.file SensitivityWatchEventModifier]
           [java.io File]
           [java.nio.file Path Paths Files StandardWatchEventKinds WatchKey ClosedWatchServiceException
            WatchEvent FileVisitor FileVisitResult]))

(defn- register-watches! [watch-service path]
  (Files/walkFileTree path
                      (reify
                        FileVisitor
                        (preVisitDirectory [_ dir _]
                          (let [^Path dir dir]
                            (. dir
                               (register watch-service
                                         (into-array [StandardWatchEventKinds/ENTRY_CREATE
                                                      StandardWatchEventKinds/ENTRY_DELETE
                                                      StandardWatchEventKinds/ENTRY_MODIFY])
                                         (into-array [SensitivityWatchEventModifier/HIGH]))))
                          FileVisitResult/CONTINUE)
                        (postVisitDirectory [_ dir exc]
                          FileVisitResult/CONTINUE)
                        (visitFile [_ file attrs]
                          FileVisitResult/CONTINUE)
                        (visitFileFailed [_ file exc]
                          FileVisitResult/CONTINUE))))

(defn watch-files! [source-paths]
  (let [file-systems (->> (for [source-path source-paths]
                            (let [path (Paths/get (.toURI (io/file source-path)))]
                              {(.getFileSystem path) [path]}))

                          (apply merge-with concat))

        watch-services (for [[file-system paths] file-systems]
                         (reduce (fn [watch-service path]
                                   (doto watch-service
                                     (register-watches! path)))
                                 (.newWatchService file-system)
                                 paths))

        thread-ch (a/chan)
        out-ch (a/chan)
        latch-ch (a/chan)]

    (go
      (a/<! latch-ch)
      (doseq [watch-service watch-services]
        (.close watch-service))
      (a/close! out-ch))

    (doseq [watch-service watch-services]
      (a/thread
        (loop []
          (when-let [watch-key (try
                                 (.take watch-service)
                                 (catch ClosedWatchServiceException e
                                   ;; we've been told to stop.
                                   ))]

            (when (some (fn [^WatchEvent e]
                          (let [fstr (.. e context toString)]
                            (and (or (. fstr (endsWith "cljs"))
                                     (. fstr (endsWith "cljc"))
                                     (. fstr (endsWith "clj"))
                                     (. fstr (endsWith "js")))
                                 (not (. fstr (startsWith ".#"))))))
                        (seq (.pollEvents watch-key)))

              (a/>!! out-ch :alert))

            (when (.reset watch-key)
              (recur))))))

    {:out-ch out-ch
     :latch-ch latch-ch}))

(comment
  (def foo-watch
    (watch-files! (io/file "/tmp/watch")))

  (a/<!! (:out-ch foo-watch))

  (a/close! (:latch-ch foo-watch)))
