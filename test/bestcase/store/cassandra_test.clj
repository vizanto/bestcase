(ns bestcase.store.cassandra-test
  (:use [midje.sweet]
        [bestcase.store.store-test-util]
        [bestcase.store.cassandra])
  (:require [containium.systems :refer (with-systems)]
            [containium.systems.config :as config]
            [containium.systems.logging :as logging]
            [containium.systems.cassandra.embedded :as embedded]
            [clojure.test :refer (deftest)]))

(deftest all-tests
  (with-systems sys [:config (config/map-config {:cassandra {:config-file "cassandra-test.yaml"}
                                                 :alia {:contact-points ["localhost"]}})
                     :logging logging/logger
                     :embedded embedded/embedded]
    (let [store (create-cassandra-store (:embedded sys))]
      (all-store-tests store))))
