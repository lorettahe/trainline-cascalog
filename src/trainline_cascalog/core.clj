(ns trainline-cascalog.core
  (:require [cascalog.api :refer [?-]]
            [clojure.tools.cli :as cli]
            [cascalog.more-taps :refer [hfs-delimited]]
            [trainline-cascalog.tracks :refer [event-generator]]
            [trainline-cascalog.sessions :refer [user-sessions-generator]]
            [trainline-cascalog.longest-sessions :refer [longest-sessions-generator]]
            [trainline-cascalog.top-tracks :refer [top-tracks]])
  (:gen-class))

(def cli-options
  [["-h" "--help" "Show usage instructions" :id :help]
   [nil "--n-sessions" "Number of top sessions to calculate top tracks over"
    :id :n-sesisons :default 50 :parse-fn #(Integer/parseInt %)]
   [nil "--n-tracks" "Number of top tracks to return"
    :id :n-tracks :default 10 :parse-fn #(Integer/parseInt %)]])

;;; Currently not using these params, as trying to feed static variables into aggregators is annoying
;;; Check with interviewer tomorrow

(defn -main
  [in out & args]
  (let [{:keys [options _ summary]} (cli/parse-opts args cli-options)]
    (if (:help options)
      (println summary)
      (?- (hfs-delimited out)
          (-> (event-generator in)
              user-sessions-generator
              longest-sessions-generator
              top-tracks)))))
