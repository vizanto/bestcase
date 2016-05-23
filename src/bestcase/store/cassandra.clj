(ns bestcase.store.cassandra
  (:require [bestcase.core :as b]
            [containium.systems.cassandra :refer (prepare do-prepared has-keyspace? write-schema
                                                  bytebuffer->bytes bytes->bytebuffer)]
            [clojure.java.io :refer (resource)]
            [taoensso.nippy :refer (freeze thaw)]))

(defn ^String str<- [value]
  (if (instance? clojure.lang.Named value) (name value) #_else (str value)))

(def cassandra-options {:consistency :one})

(defn thaw-buffer [^java.nio.ByteBuffer bytes]
  (-> bytes
      .slice
      bytebuffer->bytes
      thaw))

(defn- rows->map-of-tests [rows]
  (->> rows
       (map (fn [{:strs [test_name descriptor] :as row}]
              (when descriptor
                [(keyword test_name) (thaw-buffer descriptor)])))
       (remove nil?)
       (into {})))


(deftype CassandraStore
  [cassandra
   q?-get-test,      q?-set-test!
   q?-all-active,    q?-all-inactive
   q?-end-test!
   q?-get-altname,   q?-set-altname!
   q?-get-count,
   q?-get-counts,    q?-get-scores
   q?-inc-count!,    q?-inc-score!
   q?-get-usrscores, q?-inc-usrscore!]

  b/Store
  (get-test [store test-name]
    "Get a test's descriptor."
    (some-> (do-prepared cassandra q?-get-test cassandra-options [(str<- test-name)])
            first
            ^java.nio.ByteBuffer (get "descriptor")
            thaw-buffer))

  (set-test! [store test-name descriptor]
    "Set an active test's descriptor (not per-user) by merging with
     the current descriptor."
     (do-prepared cassandra q?-set-test! cassandra-options
                  [(bytes->bytebuffer (freeze descriptor)) (str<- test-name)])
     1)

  (get-all-active-tests [store]
    "Get the descriptors for all tests that have not been ended."
    (rows->map-of-tests (do-prepared cassandra q?-all-active cassandra-options)))

  (get-all-inactive-tests [store]
    "Get the descriptors for all tests that have been ended."
    (rows->map-of-tests (do-prepared cassandra q?-all-inactive cassandra-options)))

  (end-test! [store test-name]
    "End a test."
    (do-prepared cassandra q?-end-test! cassandra-options [(str<- test-name)]))

  (get-alternative-name [store test-name test-identity]
    "Get the alternative-name for a test-identity."
    (some-> (do-prepared cassandra q?-get-altname cassandra-options [(str<- test-name) (str<- test-identity)])
            first (get "alternative_name") keyword))

  (set-alternative-name! [store test-name test-identity alternative-name]
    "Set the alternative-name for a test-identity."
    (do-prepared cassandra q?-set-altname! cassandra-options [(str<- alternative-name) (str<- test-name) (str<- test-identity)])
    "OK")

  (get-count [store test-name alternative-name]
    "Get the number of times this test-alternative has been run."
    (-> (do-prepared cassandra q?-get-count cassandra-options [(str<- test-name) (str<- alternative-name)])
        first (get "count" 0)))

  (get-all-counts [store test-name]
    "Get the number of times every one of the test's
     alternatives have been run."
     (->> (do-prepared cassandra q?-get-counts cassandra-options [(str<- test-name)])
          (map (fn [{:strs [alternative_name count]}]
                 (when count [(keyword alternative_name) count])))
          (remove nil?)
          (into {})))

  (get-all-scores [store test-name]
    "Get the scores for all of the test's alternatives."
    (->> (do-prepared cassandra q?-get-scores cassandra-options [(str<- test-name)])
          (map (fn [{:strs [alternative_name score]}]
                 (when score [(keyword alternative_name) score])))
          (remove nil?)
          (into {})))

  (increment-count! [store test-name alternative-name]
    "Increment the count of the number of times this test-alternative
     has been run."
    (do-prepared cassandra q?-inc-count! cassandra-options [(str<- test-name) (str<- alternative-name)])
    1)

  (increment-score! [store test-name alternative-name]
    "Increment the score for this test-goal-alternative."
    (do-prepared cassandra q?-inc-score! cassandra-options [(str<- test-name) (str<- alternative-name)])
    1)

  (get-user-scored-count [store test-name goal-name test-identity]
    "How many times a user scored for a particular test-goal."
    (some-> (do-prepared cassandra q?-get-usrscores cassandra-options [(str<- test-name) (str<- goal-name) (str<- test-identity)])
            first (get "count")))

  (increment-user-scored! [store test-name goal-name test-identity]
    "Set that a user scored for a particular test-goal."
    (some-> (do-prepared cassandra q?-inc-usrscore! cassandra-options [(str<- test-name) (str<- goal-name) (str<- test-identity)])
            first (get "score"))))

(defn create-tables! [cassandra]
  (when-not (has-keyspace? cassandra "bestcase")
    (write-schema cassandra (slurp (resource "bestcase-cassandra.cql")))))


(defn create-cassandra-store
  "Takes cassandra prepare and query functions and returns a cassandra-backed Store."
  [cassandra]
  (create-tables! cassandra)
  (CassandraStore. cassandra
    #_q?-get-test      (prepare cassandra "SELECT * FROM bestcase.tests                                WHERE test_name = ?")
    #_q?-set-test!     (prepare cassandra "UPDATE bestcase.tests SET active = true, descriptor = ?     WHERE test_name = ?")

    #_q?-all-active    (prepare cassandra "SELECT * FROM bestcase.tests                                WHERE active = true")
    #_q?-all-inactive  (prepare cassandra "SELECT * FROM bestcase.tests                                WHERE active = false")
    #_q?-end-test!     (prepare cassandra "UPDATE bestcase.tests SET active = false                    WHERE test_name = ?")

    #_q?-get-altname   (prepare cassandra "SELECT alternative_name FROM bestcase.user_alternatives     WHERE test_name = ? AND test_i = ?")
    #_q?-set-altname!  (prepare cassandra "UPDATE bestcase.user_alternatives SET alternative_name = ?  WHERE test_name = ? AND test_i = ?")

    #_q?-get-count     (prepare cassandra "SELECT count FROM bestcase.counts                           WHERE test_name = ? AND alternative_name = ?")
    #_q?-get-counts    (prepare cassandra "SELECT alternative_name,count FROM bestcase.counts          WHERE test_name = ?")
    #_q?-get-scores    (prepare cassandra "SELECT alternative_name,score FROM bestcase.counts          WHERE test_name = ?")
    #_q?-inc-count!    (prepare cassandra "UPDATE bestcase.counts SET count = count + 1                WHERE test_name = ? AND alternative_name = ?")
    #_q?-inc-score!    (prepare cassandra "UPDATE bestcase.counts SET score = score + 1                WHERE test_name = ? AND alternative_name = ?")

    #_q?-get-usrscores (prepare cassandra "SELECT score FROM bestcase.user_scores                      WHERE test_name = ? AND goal_name = ? AND test_i = ?")
    #_q?-inc-usrscore! (prepare cassandra "UPDATE bestcase.user_scores SET score = score + 1           WHERE test_name = ? AND goal_name = ? AND test_i = ?")))
