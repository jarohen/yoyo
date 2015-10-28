(ns yoyo.resource.protocols)

(defprotocol IResource
  (-close! [_]))

(defrecord Resource [v close!]
  IResource
  (-close! [_]
    (close!)))
