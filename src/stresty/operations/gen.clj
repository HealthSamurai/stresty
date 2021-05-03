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

(defn exists? [pth]
  (Files/exists pth (into-array LinkOption [])))

(defn write [^Path pth content]
  (let [file (.toString (.toAbsolutePath pth))]
    (if (exists? pth)
      (println "Skip generation! File" file "already exists.")
      (spit file content))))

(defn mkdirs [^Path pth]
  (Files/createDirectories  pth (into-array FileAttribute [])))


(defn walk [^Path pth]
  (when (exists? pth)
    (iterator-seq (.iterator (Files/walk pth (into-array FileVisitOption []))))))

(defn rmdir [^Path pth]
  (doseq [^Path p (->> (walk pth)
                 (sort-by (fn [^Path x] (.toAbsolutePath x)))
                 (reverse))]
    (println "rm" p)
    (.delete ^File (.toFile p))))


(defn gen-case [ns]
  (format "{ns %s
import #{sty}

case
 {:zen/tags #{sty/case}
  :steps [
  {:id :first-step
   :desc \"Crate Patient\"
   :do {:act sty/http
        :method :post
        :url \"/Patient\"
        :body {:resourceType \"Patient\"
               :name [{:family \"Doe\", :given [\"John\"]}]}}
   :match {:by sty/matcho
           :status sty/ok?
           :body {:id sty/string?}}}

  {:id :second-step
   :do {:act sty/print
        :path [:first-step :body]}}

  {:id :third-step
   :do {:act sty/http
        :method :get
        :url (str \"/Patient/\" (get-in sty/state [:first-step :body :id]))}
   :match {:by sty/matcho
           :status sty/ok?
           :body {:name (get-in sty/state [:first-step :body :name])}}}

  ;; add more steps
  ]}}" ns))

(defn gen-env [ns]
  (format "
{ns envs
 import #{
   sty
   %s
 }

 env {
  :zen/tags #{sty/env}
  :base-url \"http://localhost:8080\"
  ;; :basic-auth {:user \"user?\" :password \"password?\"}
  ;; :auth-token  \"????\"
 }

}
" ns))

(defn generate [ztx opts]
  (println "Generate " opts)
  (if-let [proj (:project opts)]
    (let [home (get-in @ztx [:paths 0])
          proj-path (get-path proj)
          dir-path  (path home proj-path)
          env-path  (path home ["envs.edn"])
          case-path (path home (conj proj-path "case.edn"))]
      (println "mkdirs" dir-path)
      (mkdirs dir-path)
      (println "create" (str (.toAbsolutePath ^Path case-path)))
      (write case-path (gen-case proj))
      (println "create" (str (.toAbsolutePath ^Path env-path)))
      (write env-path (gen-env proj)))
    (println "Parameter --project is required")))
