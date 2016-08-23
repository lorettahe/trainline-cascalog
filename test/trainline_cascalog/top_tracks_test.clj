(ns trainline-cascalog.top-tracks-test
  (:require [midje.sweet :refer :all]
            [midje.cascalog :refer :all]
            [trainline-cascalog.top-tracks :refer :all]))

(defn make-many-tracks
  [n]
  (repeat n [(str "a" n) (str "artist" n) (str "track" n)]))

(def mock-data
  (into [] (mapcat make-many-tracks (range 1 12))))

(def mock-expected
  (into [] (map (fn [n] [(str "a" n) (str "artist" n) (str "track" n) n]) (range 2 12))))

(fact "top-tracks should keep the top 10 tracks with most frequencies"
      (top-tracks mock-data) => (produces mock-expected))
