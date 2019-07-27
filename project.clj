(defproject clj-hua "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [clj-http "2.0.0"]
                 [hiccup "2.0.0-alpha1"]
                 [hickory "0.7.1"]
                 [http-kit "2.1.19"]
                 [org.clojure/data.codec "0.1.0"] ; base64 encoding. possibly write my own
                 [cheshire "5.3.1"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/java.jdbc "0.3.5"] ; jdbc
                 [org.xerial/sqlite-jdbc "3.7.2"] ; sqlite
                 [com.taoensso/timbre "3.1.6"] ; logging
                 [com.postspectacular/rotor "0.1.0"] ; roter logging
                 [org.clojure/tools.cli "0.3.1"] ; command line processing
                 [com.climate/claypoole "1.1.3"]
                 [org.clojars.august/sparrows "0.2.8"]]
  :main clj-hua.core)
