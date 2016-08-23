(ns trainline-cascalog.tracks
  (:require [clj-time.format :as f]
            [cascalog.api :refer [defmapfn <-]]
            [cascalog.more-taps :refer [hfs-delimited]]))

(def timestamp-format
  (f/formatter :date-time-no-ms))

(defn get-track-id
  [artist-id artist track-id track]
  (if track-id
    track-id
    (str (if artist-id artist-id artist) "||" track)))

(defmapfn parse-event
  [timestamp-raw artist-id artist track-id track]
  (let [new-track-id (get-track-id artist-id artist track-id track)
        timestamp (f/parse timestamp-format timestamp-raw)]
    [timestamp new-track-id]))

(defn event-generator
  [in]
  (<- [?user-id ?timestamp ?artist ?track-id ?track]
      ((hfs-delimited in :strict? false :classes [String String String String String String])
        ?user-id ?timestamp-raw !!artist-id ?artist !!track-id ?track)
      (parse-event ?timestamp-raw !!artist-id ?artist !!track-id ?track :> ?timestamp ?track-id)))


