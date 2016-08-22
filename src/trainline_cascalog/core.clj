(ns trainline-cascalog.core
  (:require [cascalog.api :refer [?- <- ?<- stdout defmapfn defaggregatefn]]
            [cascalog.more-taps :refer [hfs-delimited]]
            [cascalog.logic.ops :as c]
            [clj-time.core :as t]
            [clj-time.format :as f])
  (:gen-class))

(defn assoc-or-conj
  [m k v]
  (if (m k)
    (update m k conj v)
    (assoc m k [v])))

(defn get-track-id
  [artist-id artist track-id track]
  (if track-id
    track-id
    (str (if artist-id artist-id artist) "||" track)))

(def timestamp-format
  (f/formatter :date-time-no-ms))

(defn within-session?
  [previous-timestamp timestamp]
  (t/within? previous-timestamp (.plus previous-timestamp 1200000) timestamp))

(defmapfn parse-event
  [timestamp-raw artist-id artist track-id track]
  (let [new-track-id (get-track-id artist-id artist track-id track)
        timestamp (f/parse timestamp-format timestamp-raw)]
    [timestamp new-track-id]))

(defn event-tap
  [in]
  (<- [?user-id ?timestamp ?artist ?track-id ?track]
      ((hfs-delimited in :strict? false :classes [String String String String String String])
        ?user-id ?timestamp-raw !!artist-id ?artist !!track-id ?track)
      (parse-event ?timestamp-raw !!artist-id ?artist !!track-id ?track :> ?timestamp ?track-id)))

(defn add-old-session
  [{:keys [sessions session-count current-session last-timestamp] :as state}]
  {:sessions (assoc-or-conj sessions (:count current-session) current-session)
   :session-count (inc session-count)
   :current-session nil
   :last-timestamp nil})

(defn remove-short-session
  [state min-key]
  (let [short-session-count (count ((:sessions state) min-key))]
    (if (= short-session-count 1)
      (-> state (update :sessions dissoc min-key) (update :session-count dec))
      (-> state (update-in [:sessions min-key] rest) (update :session-count dec)))))

(defn discard-or-add-old-session
  [{:keys [sessions session-count current-session last-timestamp] :as state}]
  (if (< session-count 50)
    (add-old-session state)
    (let [min-session-length (apply min (keys sessions))]
      (if (> (:count current-session) min-session-length)
        (-> state
            (remove-short-session min-session-length)
            (add-old-session))
        (assoc state :current-session nil last-timestamp nil)))))

(defn create-new-session
  [state timestamp track-id artist track]
  (-> state
      (assoc :last-timestamp timestamp)
      (assoc :current-session {:count 1 :tracks [[track-id artist track]]})))

(defn add-to-current-session
  [state timestamp track-id artist track]
  (-> state
      (update-in [:current-session :count] inc)
      (update-in [:current-session :tracks] conj [track-id artist track])
      (assoc :last-timestamp timestamp)))

(defaggregatefn keep-longest-sessions
  ([] {:sessions {} :session-count 0 :current-session nil :last-timestamp nil})
  ([state timestamp track-id artist track]
    (if (or (nil? (:last-timestamp state)) (nil? (:current-session state)))
      (create-new-session state timestamp track-id artist track)
      (if-not (within-session? (:last-timestamp state) timestamp)
        (-> state
            (discard-or-add-old-session)
            (create-new-session timestamp track-id artist track))
        (add-to-current-session state timestamp track-id artist track))))
  ([state]
    (let [final-state (discard-or-add-old-session state)]
      (map (fn [session] [(:count session) (:tracks session)]) (mapcat identity (vals (:sessions final-state)))))))

(defn sessions-query
  [events]
  (<- [?user-id ?session-length ?session]
      (events ?user-id ?timestamp ?artist ?track-id ?track)
      (:sort ?timestamp)
      (keep-longest-sessions ?timestamp ?track-id ?artist ?track :> ?session-length ?session)))

(defaggregatefn get-overall-longest-sessions
  ([] {:sessions {} :session-count 0})
  ([state session-length session]
    (if (< (:session-count state) 50)
      (-> state
          (update :sessions assoc-or-conj session-length session)
          (update :session-count inc))
      (let [min-length (apply min (keys (:sessions state)))]
        (if (< min-length session-length)
          (-> state
              (remove-short-session min-length)
              (update :sessions assoc-or-conj session-length session)
              (update :session-count inc))
          state))))
  ([state]
    (mapcat identity (mapcat identity (vals (:sessions state))))))

(defn longest-sessions-query
  [sessions]
  (<- [?track-id ?artist ?track]
      (sessions ?user-id ?session-length ?session)
      (get-overall-longest-sessions ?session-length ?session :> ?track-id ?artist ?track)))

(defn tracks-tap
  [longest-sessions-query]
  (<- [?track-id ?artist ?track ?count]
      (longest-sessions-query ?track-id ?artist ?track)
      (c/count ?count)))

(defn -main
  [in & args]
  (?- (stdout)
       ;[?track-id ?artist ?track ?count]
       (c/first-n (tracks-tap (longest-sessions-query (sessions-query (event-tap in))))
                  10
                  :sort ["?count"]
                  :reverse true)))
