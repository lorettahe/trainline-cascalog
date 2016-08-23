(ns trainline-cascalog.utils)

(defn assoc-or-conj
  [m k v]
  (if (m k)
    (update m k conj v)
    (assoc m k [v])))
