(ns trainline-cascalog.utils-test
  (:require [midje.sweet :refer :all]
            [trainline-cascalog.utils :refer :all]))

(facts "assoc-or-conj test"
       (fact "assoc-or-conj should assoc k with [v] into m if k doesn't already exist in m"
             (assoc-or-conj {:a [1]} :b 2) => {:a [1] :b [2]})
       (fact "assoc-or-conj should conj v into value corresponding to k if k exists in m"
             (assoc-or-conj {:a [1]} :a 2) => {:a [1 2]}))
