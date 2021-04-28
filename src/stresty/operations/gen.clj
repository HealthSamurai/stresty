(ns stresty.operations.gen
  (:require [clojure.string :as str])
  (:import
   (java.io File)
   (java.nio.file Files Path Paths OpenOption FileVisitOption LinkOption)
   (java.nio.file.attribute FileAttribute)
   (java.net URI)))

(set! *warn-on-reflection* true)

(defn get-path [ns]
  (->> (str/split ns #"\.")
       (into [])))

(comment
  (get-path "aidbox.tests")
  (Paths/get ".tmp" (into-array ["myproj" "ns"]))

  (Files/createDirectories (Paths/get ".tmp" (into-array ["myproj" "ns"])) (into-array FileAttribute []))

  (.toString (.toAbsolutePath (Paths/get ".tmp" (into-array ["myproj" "ns" "case.edn"]))))

  (spit (.toString (.toAbsolutePath (Paths/get ".tmp" (into-array ["myproj" "ns" "case.edn"]))))
        "{}"))

(defn path [home pth]
  (Paths/get home (into-array String (into [] pth))))

(defn write [^Path pth content]
  (spit (.toString (.toAbsolutePath pth)) content))

(defn mkdirs [^Path pth]
  (Files/createDirectories  pth (into-array FileAttribute [])))

(defn exists? [pth]
  (Files/exists pth (into-array LinkOption [])))

(defn walk [^Path pth]
  (when (exists? pth)
    (iterator-seq (.iterator (Files/walk pth (into-array FileVisitOption []))))))

(defn rmdir [^Path pth]
  (doseq [^Path p (->> (walk pth)
                 (sort-by (fn [^Path x] (.toAbsolutePath x)))
                 (reverse))]
    (println "rm" p)
    (.delete ^File (.toFile p))))


(defn generate [ztx opts]
  (println "Generate " opts)
  (let [home (get-in @ztx [:paths 0])
        proj (:project opts)
        proj-path (get-path proj)
        dir-path  (path home proj-path)
        env-path  (path home ["envs.edn"])
        case-path (path home (conj proj-path "case.edn"))]
    (println "mkdirs" dir-path)
    (mkdirs dir-path)
    (println "create" (str (.toAbsolutePath ^Path case-path)))
    (write case-path "{}")
    (println "create" (str (.toAbsolutePath ^Path env-path)))
    (write env-path "{}")))
