(ns BioClojure.sampleSchedule)


(defstruct schedule :points :num-dimensions)


;;MATT This is good shit.   Can YOU RENAME THIS FILE TO START with NMR like YOUR OTHER ONES ...  ?
;;bash> git mv sampleSchedule.clj nmr_sampleSchedule.clj
;;bash> git add nmr_sampleSchedule.clj
;;bash> git commit 
;;bash> git push 
(defn make-schedule
  [ point-map num-dimensions ]
  (let [points (keys point-map)
        is-dims-correct? (fn [pt] 
                           (= num-dimensions 
                             (count (pt :coordinates)) ; is the number of coordinates correct?
                             (count (pt :quadrature))))] ; is the number of quad things correct?
    (if (every? is-dims-correct? points)
      (struct schedule point-map num-dimensions)
      "oops -- there was a problem")))


(defn uniformly-sampled-grid
  [ xlow xhigh ylow yhigh ]
  (for [x (range xlow xhigh) 
        y (range ylow yhigh)]
    {:coordinates (list x y) :quadrature (list \R \R)})) ;; I need this in a map ....


(def example 
  (make-schedule
    (into {} (map (fn [x] [x 1]) mylist)) ; convert list to map (with default values of 1)
    2))