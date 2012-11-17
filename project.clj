(defproject isbnnetinclj "0.1.0-SNAPSHOT"
            :description "A quick way to find the online prices for a book in India"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [org.clojure/data.json "0.1.3"]
                           [org.clojure/core.cache "0.5.0"]
                           [noir "1.3.0-beta7"]
                           [com.novemberain/monger "1.0.0-rc2"]
                           [clj-time "0.4.2"]
                           [enlive "1.0.0"]
                           [stencil "0.3.0-preview1"]
                           [timbre "0.5.1-SNAPSHOT"]]
            :plugins [[lein-swank "1.4.4"]]
            :main isbnnetinclj.server
            :license {:name "Eclipse Public License - v 1.0"
                      :url "http://www.eclipse.org/legal/epl-v10.html"
                      :distribution :repo
                      :comments "same as Clojure"}
            :min-lein-version "2.0.0")
