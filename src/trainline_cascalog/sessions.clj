(ns trainline-cascalog.sessions
  (:require [clj-time.core :as t]
            [cascalog.api :refer [defaggregatefn <-]]
            [cascalog.logic.ops :as c]
            [trainline-cascalog.utils :refer [assoc-or-conj]]))

(defn within-session?
  [previous-timestamp timestamp]
  (t/within? previous-timestamp (.plus previous-timestamp 1200000) timestamp))

(defn add-old-session
  [{:keys [sessions session-count current-session]}]
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
        (assoc state :current-session nil :last-timestamp nil)))))

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

(defn user-sessions-generator
  [events-generator]
  (<- [?user-id ?session-length ?session]
      (events-generator ?user-id ?timestamp ?artist ?track-id ?track)
      (:sort ?timestamp)
      (keep-longest-sessions ?timestamp ?track-id ?artist ?track :> ?session-length ?session)))
