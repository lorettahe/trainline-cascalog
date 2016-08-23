(ns trainline-cascalog.sessions-test
  (:require [midje.sweet :refer :all]
            [midje.cascalog :refer :all]
            [cascalog.api :refer [?- stdout]]
            [clj-time.core :as t]
            [trainline-cascalog.sessions :refer :all]))

(facts "within-session? test"
       (fact "within-session? should return true if the timestamps are less than 20 minutes apart, false otherwise"
             (within-session? (t/date-time 2006 8 23 15 20 39)
                              (t/date-time 2006 8 23 15 40 38)) => true

             (within-session? (t/date-time 2006 8 23 15 20 29)
                              (t/date-time 2006 8 23 15 40 29)) => true

             (within-session? (t/date-time 2006 8 23 23 51 40)
                              (t/date-time 2006 8 24 0 1 30)) => true

             (within-session? (t/date-time 2006 8 23 15 20 39)
                              (t/date-time 2006 8 23 15 40 40)) => false))

(facts "add-old-session test"
       (fact "add-old-session should return a state with current session added in"
             (add-old-session {:sessions        {4 [{:count 4 :tracks [:track1]}]}
                               :session-count   1
                               :current-session {:count 5 :tracks [:track2]}
                               :last-timestamp  (t/now)})
             => {:sessions        {4 [{:count 4 :tracks [:track1]}]
                                   5 [{:count 5 :tracks [:track2]}]}
                 :session-count   2
                 :current-session nil :last-timestamp nil}

             (add-old-session {:sessions        {4 [{:count 4 :tracks [:track1]}]}
                               :session-count   1
                               :current-session {:count 4 :tracks [:track2]}
                               :last-timestamp  (t/now)})
             => {:sessions        {4 [{:count 4 :tracks [:track1]}
                                      {:count 4 :tracks [:track2]}]}
                 :session-count   2
                 :current-session nil :last-timestamp nil}))

(facts "remove-short-session test"
       (fact "remove-short-session should remove the first session in the list corresponding to the given length key,
       if there is more than 1 session stored in :sessions under that key"
             (remove-short-session {:sessions        {1 [{:count 1 :tracks [:track1]}
                                                         {:count 1 :tracks [:track2]}]
                                                      2 [{:count 2 :tracks [:track1 :track2]}]}
                                    :session-count   3
                                    :current-session :arbitrary
                                    :last-timestamp  :arbitrary}
                                   1)
             => {:sessions        {1 [{:count 1 :tracks [:track2]}]
                                   2 [{:count 2 :tracks [:track1 :track2]}]}
                 :session-count   2
                 :current-session :arbitrary :last-timestamp :arbitrary})

       (fact "remove-short-session should remove the length key entirely if there is only one session left under that
       length key"
             (remove-short-session {:sessions        {1 [{:count 1 :tracks [:track1]}]
                                                      2 [{:count 2 :tracks [:track1 :track2]}]}
                                    :session-count   2
                                    :current-session :arbitrary
                                    :last-timestamp  :arbitrary}
                                   1)
             => {:sessions        {2 [{:count 2 :tracks [:track1 :track2]}]}
                 :session-count   1
                 :current-session :arbitrary :last-timestamp :arbitrary}))

(facts "discard-or-add-old-session test"
       (fact "discard-or-add-old-session should add in a session if the :session-count value in state is smaller than 50"
             (discard-or-add-old-session {:sessions        {1 [{:count 1 :tracks [:track1]}]}
                                          :session-count   49
                                          :current-session {:count 1 :tracks [:track2]}
                                          :last-timestamp  :arbitrary})
             => {:sessions        {1 [{:count 1 :tracks [:track1]}
                                      {:count 1 :tracks [:track2]}]}
                 :session-count   50
                 :current-session nil :last-timestamp nil})

       (fact "discard-or-add-old-session should discard the current session if its session length is smaller than the
        min length of stored sessions, and :session-count value in state is >= 50"
             (discard-or-add-old-session {:sessions        {2 [{:count 2 :tracks [:track1 :track2]}]}
                                          :session-count   50
                                          :current-session {:count 1 :tracks [:track1]}
                                          :last-timestamp  :arbitrary})
             => {:sessions        {2 [{:count 2 :tracks [:track1 :track2]}]}
                 :session-count   50
                 :current-session nil :last-timestamp nil})

       (fact "discard-or-add-old-session should remove the first session with min stored session length and add in the
       current session if current session length is larger than the stored min length, and :session-count value in state
       is >= 50"
             (discard-or-add-old-session {:sessions        {1 [{:count 1 :tracks [:track1]}]
                                                            2 [{:count 2 :tracks [:track1 :track2]}]}
                                          :session-count   50
                                          :current-session {:count 2 :tracks [:track3 :track4]}
                                          :last-timestamp  :arbitrary})
             => {:sessions        {2 [{:count 2 :tracks [:track1 :track2]}
                                      {:count 2 :tracks [:track3 :track4]}]}
                 :session-count   50
                 :current-session nil :last-timestamp nil}))

(facts "create-new-session test"
       (fact "create-new-session should start up a new session in the state"
             (create-new-session {:sessions        :arbitrary :session-count :arbitrary
                                  :current-session :arbitrary :last-timestamp :arbitrary}
                                 (t/date-time 2008 8 23 15 23 14)
                                 "a111" "artist name" "track name")
             => {:sessions        :arbitrary
                 :session-count   :arbitrary
                 :current-session {:count 1 :tracks [["a111" "artist name" "track name"]]}
                 :last-timestamp  (t/date-time 2008 8 23 15 23 14)}))

(facts "add-to-current-session test"
       (fact "add-to-current session should add a new track into the current session and update the last timestamp"
             (add-to-current-session {:sessions        :arbitrary :session-count :arbitrary
                                      :current-session {:count 1 :tracks [["a111" "artist name" "track name"]]}
                                      :last-timestamp  :arbitrary}
                                     (t/date-time 2008 8 23 15 23 14)
                                     "a222" "artist2 name" "track2 name")
             => {:sessions        :arbitrary
                 :session-count   :arbitrary
                 :current-session {:count 2 :tracks [["a111" "artist name" "track name"]
                                                     ["a222" "artist2 name" "track2 name"]]}
                 :last-timestamp  (t/date-time 2008 8 23 15 23 14)}))

(defn generate-session-tracks
  [user-id n start-timestamp]
  (into []
        (rest (reductions
                (fn [[user-id timestamp artist track-id track] _]
                  [user-id (.plus timestamp (long (rand-int 1200000))) artist track-id track])
                [user-id start-timestamp "artist" "track-id" "track"] (range n)))))

(def mock-track-tap
  (into [] (concat (generate-session-tracks "user1" 2 (t/date-time 2008 8 23 15 23 14))
                   (generate-session-tracks "user2" 1 (t/date-time 2008 8 23 15 23 14))
                   (generate-session-tracks "user1" 3 (t/date-time 2009 8 23 16 57 39)))))

(fact "user-sessions-generator should aggregate and keep sessions from each user"
      (user-sessions-generator mock-track-tap)
      => (produces [["user1" 2 [["track-id" "artist" "track"]
                                ["track-id" "artist" "track"]]]
                    ["user2" 1 [["track-id" "artist" "track"]]]
                    ["user1" 3 [["track-id" "artist" "track"]
                                ["track-id" "artist" "track"]
                                ["track-id" "artist" "track"]]]]))