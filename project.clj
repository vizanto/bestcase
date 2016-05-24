(defproject vizanto/bestcase "0.2.2.3"
  :description "An A/B testing library for clojure"
  :url "https://github.com/charlespwd/bestcase"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.incubator "0.1.3"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.taoensso/carmine "2.12.2"]
                 [containium "0.1.0-SNAPSHOT"]
                 [ring "1.3.2"]
                 [compojure "1.3.1"]
                 [hiccup "1.0.5"]]
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]
                        [midje "1.6.3" :exclusions [org.clojure/clojure]]
                        [com.stuartsierra/lazytest "1.2.3"]]
         :plugins [[lein-midje "3.1.3"]
                   [codox "0.8.10"]]
         :codox {:exclude [bestcase.for-testing]
                 :src-dir-uri
                 "http://github.com/charlespwd/bestcase/tree/master/"
                 :src-linenum-anchor-prefix "L"}
         :repositories {"stuart" "http://stuartsierra.com/maven2"}}})

