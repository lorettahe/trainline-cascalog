(ns trainline-cascalog.longest-sessions-test
  (:require [midje.sweet :refer :all]
            [midje.cascalog :refer :all]
            [trainline-cascalog.longest-sessions :refer :all]))

(facts "longest-sessions-generator should keep the longest 50 sessions"
       (longest-sessions-generator [["user1" 3 [["id1" "artist1" "track1"]
                                                ["id2" "artist2" "track2"]
                                                ["id3" "artist3" "track3"]]]
                                    ["user2" 2 [["id1" "artist1" "track1"]
                                                ["id2" "artist2" "track2"]]]
                                    ["user3" 1 [["id1" "artist1" "track1"]]]])
       => (produces [["id1" "artist1" "track1"]
                     ["id2" "artist2" "track2"]
                     ["id3" "artist3" "track3"]
                     ["id1" "artist1" "track1"]
                     ["id2" "artist2" "track2"]
                     ["id1" "artist1" "track1"]
                     ]))
