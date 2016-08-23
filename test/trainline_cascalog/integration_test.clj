(ns ^:integration trainline-cascalog.integration-test
  (:require [trainline-cascalog.core :refer [-main]]
            [midje.sweet :refer :all]
            [clojure.java.shell :refer [sh]]))

(fact :integration
      (sh "rm" "-rf" "test-resources/integration-out")
      (-main "test-resources/test100.tsv" "test-resources/integration-out")
      (let [expected (slurp "test-resources/integration-expected-out/part-00000")]
        (slurp "test-resources/integration-out/part-00000") => expected))



