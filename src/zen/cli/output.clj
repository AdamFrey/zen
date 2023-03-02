(ns zen.cli.output
  (:require clojure.pprint))

(def ansi
  {:reset       "\u001B[0m"
   :format-bold "\u001B[1m"})

(defn print-table
  ;; Custom clojure.pprint/print-table
  ([ks rows]
     (when (seq rows)
       (let [widths (map
                     (fn [k]
                       (apply max (count (str k)) (map #(count (str (get % k))) rows)))
                     ks)
             spacers (map #(apply str (repeat % "-")) widths)
             fmts (map #(str "%-" % "s") widths)
             fmt-row (fn [leader divider trailer row]
                       (apply str (interpose divider
                                             (for [[col fmt] (map vector (map #(get row %) ks) fmts)]
                                               (format fmt (str col))))))]
         (doseq [row rows]
           (println (fmt-row " " " " " " row))))))
  ([rows] (print-table (keys (first rows)) rows)))

(defn get-format
  [arguments]
  (some->>
   (filter #(clojure.string/starts-with? % "--format=") arguments)
   (first)
   (re-find #"--format=(.*)")
   (last)
   (keyword)))

(defmulti return
  (fn [data]
    (:format data))
  :default :pprint)

(defmethod return :identity
  [data]
  data)

(defmethod return :pprint
  [data]
  (clojure.pprint/pprint data))

(defmethod return :command
  [{result :zen.cli/result}]
  (println (str (:format-bold ansi) "Description:" (:reset ansi)))
  (println "" (:description result))
  (println (str (:format-bold ansi) "Usage:" (:reset ansi)))
  (print-table
   (for [usage (:usage result)]
     {:command (->>
                [" "
                 (clojure.string/join " " (:path usage))
                 (some->>
                  (:params usage)
                  (seq)
                  (mapv #(str "[" % "]"))
                  (clojure.string/join " ")
                  (str " "))]
                (remove empty?)
                (apply str))
      :description 
      (when (:description usage)
        (str "- " (:description usage)))})))
