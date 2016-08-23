(ns trainline-cascalog.tracks-test
  (:require [midje.sweet :refer :all]
            [trainline-cascalog.tracks :refer :all]
            [clj-time.core :as t]
            [midje.cascalog :refer :all]
            [cascalog.more-taps :refer [hfs-delimited]]))

(facts "get-track-id should return track-id if already there, otherwise make it up with artist(-id) and track name"
       (fact "get-track-id should return track-id if already there"
             (get-track-id "a" "b" "a111" "name") => "a111")
       (fact "get-track-id should return artist-id||track name if artist-id is given"
             (get-track-id "artist1" "artist name" nil "name") => "artist1||name")
       (fact "get-track-id should return artist||track name if artist-id isn't given"
             (get-track-id nil "artist name" nil "song name") => "artist name||song name"))

(fact "event-generator should return a single tuple containing [?user-id ?timestamp ?artist ?track-id ?track]
       for each line coming in from input"
      (event-generator :in) => (produces [["user1" (t/date-time 2006 8 23 15 12 13) "artist name" "a111" "track name"]
                                          ["user1" (t/date-time 2006 8 23 15 21 13) "artist name" "artist1||track name" "track name"]
                                          ["user2" (t/date-time 2006 8 23 15 19 28) "artist name" "artist name||track name" "track name"]])
      (provided
        (hfs-delimited :in :strict? false :classes [String String String String String String])
        => [["user1" "2006-08-23T15:12:13Z" "artist1" "artist name" "a111" "track name"]
            ["user1" "2006-08-23T15:21:13Z" "artist1" "artist name" nil "track name"]
            ["user2" "2006-08-23T15:19:28Z" nil "artist name" nil "track name"]]))


