(ns BioClojure.vennnmr
  (:use [clojure.contrib.str-utils2 :only (split-lines trim)])
  (:use clojure.contrib.generic.functor))

;; @Matt:  suggested refactorings:
;; refactor and post data model -- make sure all code uses it
;; clarify, post general procedure for merging these data (sequence, shifts, shift-stats)
;; clojure-idiomatize code


(defn parse-bmrb-stats
  "output: list of maps
     each map: keys are /Res, Name, Atom, Count, Min. Max., Avg., StdDev/"
  [string]
  (let [lines (split-lines string)]
   (let [headings (.split (first lines) " +")
         atomlines (rest lines)]
    (for [atomline atomlines]
     (zipmap headings (.split atomline " +"))))))

(defn parse-sequence
  "input: cyana-formatted sequence string -- residues separted by newlines
   output: map -- key is index, value is amino acid type
     length is same as number of lines in input string"
  [string]
  (zipmap (map #(str (+ % 1)) (range)) ; need indices to be 1-indexed
          ;; should refactor to remove 'str' .... requires other changes, too
          (split-lines (.trim string))))

(defn parse-shifts
  "output: list of maps
     each map:  keys are /id, shift, error, atom, resid/"
  [string]
  (let [headers [:id :shift :error :atom :resid]]
   (map #(zipmap headers (.split (trim %) " +")) 
         (split-lines string))))

(defn seq-to-protein
  "input: map of index to aatype
   output: map of index to residue"
  [seqmap]
  (let [func (fn [aatype] {:aatype aatype :atoms {}})]
   (fmap func seqmap)))

(defn place-shift
  "input: a protein (map) and a cyana line map
   output:  a protein with an added atom"
  [prot shift-map]
  (let [resid (shift-map :resid)
        atomname (shift-map :atom)
        shift (shift-map :shift)]
   (assoc prot     ;; clearly there's a big problem with this code
          resid    ;; the goal is to change something a couple of map layers down
          (assoc (prot resid)  ;; but I don't know how to do it nicely in Clojure
                 :atoms        ;; should I use zippers?
                 (assoc ((prot resid) :atoms) 
                        atomname 
                        shift)))))

(defn merge-shifts
  "input: protein, list of maps of header to value"
  [prot shift-list]
  (reduce place-shift prot shift-list))

(defn place-stats
  "marked for refactoring:  what data model does this use?
   input: residue, average shifts map (specific to that residue) -- keys: atomname, values: map maybe?"
  [res avg-shifts]
  (let [my-func (fn [atommap] (into atommap [[:stdshift (avg-shifts (atommap ??atomname??))]])) ] ; this function takes an atommap, looks up the atomname in avg-shifts, and gets *some* information (whatever *some* means)
   (fmap my-func res))) ;; does fmap work here ???? need the atom name to look up the shift

(defn merge-bmrb
  "input: bmrb stats, protein with chemical shifts of assigned atoms
   output: protein with bmrb stats for each atom that initially had an assignment"
  [avg-shifts prot]
    ;; for each residue in the protein
    ;; find the matching shifts in the average stats
    ;;      use the aatype of the residue to look up the stats
    ;;   then for each atom in the residue
    ;;     look up the shift
    ;;     place it in the dictionary for the protein's atom .... or something
  (let [selector (fn [res] (place-stats res 
                                        (avg-shifts (res :aatype))))]
   (fmap selector prot)))

(defn transform-stats
  "input: list of maps based on bmrb shift statistics file
   output: map of keys -- amino acid type names, values maps of keys -- atom names, values maps of -- stats about that atom in that amino acid (avg shift, std dev, etc.)"
  [shift-stats]
  (let [f (fn [base linemap] base)] ;; this is not done -- function should create new amino acid if it doesn't exist, then insert atom and its values into that amino acid
   (reduce f {} shift-stats))) 

(defn venn-nmr-help
  [seqstr shiftstr bmrbstr]
  (let [seq (parse-sequence seqstr)               ;; parse the sequence string (comments need refactoring)
        shifts (parse-shifts shiftstr)            ;; parse the assigned shifts string
        avg-shifts (transform-stats (parse-bmrb-stats bmrbstr))]    ;; parse the bmrb stats string
   (let [prot (seq-to-protein seq)]               ;; create a protein object from the sequence
    (let [prot-shifts (merge-shifts prot shifts)] ;; put the assigned chemical shifts into the protein
     (merge-bmrb avg-shifts prot-shifts)))))      ;; put the bmrb standard shifts into the protein (based on matching aatype/atomtype)

(defn venn-nmr
  [seqfile shiftfile bmrbfile]
  (venn-nmr-help (.slurp seqfile)
                 (.slurp shiftfile)
                 (.slurp bmrbfile)))


;; for informal testing
(def shifts (parse-shifts "     7  57.135   0.000 CA      1
     1   4.155   0.000 HA      1
     8  29.815   0.000 CB      1
     2   1.908   0.000 HB2     1
     3   2.003   0.000 HB3     1
     9  36.326   0.000 CG      1
     4   2.222   0.000 HG2     1
     5   2.222   0.000 HG3     1
     6 176.410   0.000 C       1
    19 118.889   0.000 N       2
    10   8.376   0.000 H       2
    17  53.005   0.000 CA      2
    11   4.752   0.000 HA      2
    18  38.025   0.000 CB      2
    12   2.675   0.000 HB2     2
    13   2.932   0.000 HB3     2
    20 110.708   0.000 ND2     2
    14   6.792   0.000 HD21    2
    15   7.423   0.000 HD22    2
    16 175.445   0.000 C       2"))

(def myseq (parse-sequence "MET
ASN
CYS
VAL
CYS
GLY
SER
GLY
LYS
THR
TYR
ASP
ASP
CYS
CYS
GLY
PRO
LEU"))

(def stats (parse-bmrb-stats "Res    Name     Atom     Count        Min.        Max.       Avg.      StdDev
ALA     H        H        30843        3.53        11.48       8.20       0.60       
ALA     HA       H        23429        0.87         6.51       4.26       0.44       
ALA     HB       H        22202       -0.88         3.12       1.35       0.26       
ALA     C        C        19475      164.48       187.20     177.72       2.14       
ALA     CA       C        26260       44.22        65.52      53.13       1.98       
ALA     CB       C        24766        0.00        38.70      19.01       1.84       
ALA     N        N        28437       77.10       142.81     123.24       3.54"))

(def myprot (seq-to-protein myseq))
(def merged (merge-shifts myprot shifts))


