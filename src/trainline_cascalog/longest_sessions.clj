(ns trainline-cascalog.longest-sessions
  (:require [cascalog.api :refer [defaggregatefn <-]]
            [trainline-cascalog.sessions :refer [remove-short-session]]
            [trainline-cascalog.utils :refer [assoc-or-conj]]))

(defn enough-sessions?
  [session-count sessions-no-limit]
  (< session-count sessions-no-limit))

(defaggregatefn get-overall-longest-sessions
  ([] {:sessions {} :session-count 0})
  ([state session-length session]
    (if (enough-sessions? (:session-count state) 50)
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

(defn longest-sessions-generator
  [sessions]
  (<- [?track-id ?artist ?track]
      (sessions ?user-id ?session-length ?session)
      (get-overall-longest-sessions ?session-length ?session :> ?track-id ?artist ?track)))
