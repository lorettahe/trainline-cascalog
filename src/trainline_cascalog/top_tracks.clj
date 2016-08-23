(ns trainline-cascalog.top-tracks
  (:require [cascalog.api :refer [<-]]
            [cascalog.logic.ops :as c]))

(defn tracks-freq-generator
  [longest-sessions-query]
  (<- [?track-id ?artist ?track ?count]
      (longest-sessions-query ?track-id ?artist ?track)
      (c/count ?count)))

(defn top-tracks
  [longest-sessions-query]
  (c/first-n (tracks-freq-generator longest-sessions-query)
             10
             :sort ["?count"]
             :reverse true))


