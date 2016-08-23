(defproject trainline-cascalog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories  {"conjars" "http://conjars.org/repo/"}
  :aot  [trainline-cascalog.core]
  :main trainline-cascalog.core
  :aliases {"test-core" ["midje" ":filter" "-integration"]
            "test-all"  ["midje"]
            "test-integration" ["midje" ":filter" "integration"]}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [cascading/cascading-hadoop2-mr1 "2.5.3"]
                 [cascalog/cascalog-core "3.0.0" :exclusion [[cascading/cascading-hadoop]
                                                             [cascading/cascading-local]]]
                 [cascalog/cascalog-more-taps "3.0.0"]
                 [org.apache.hadoop/hadoop-common "2.7.2"]
                 [org.apache.hadoop/hadoop-mapreduce-client-jobclient "2.7.2"]
                 [clj-time "0.12.0"]]
  :uberjar-exclusions [#"META-INF/LICENSE"]
  :profiles
  {:dev {:dependencies [[cascalog/midje-cascalog "3.0.0"]]
         :plugins      [[lein-midje "3.2"]]
         :resource-paths ["test-resources"]}})
