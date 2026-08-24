(defproject todo-clojure "0.1.0-SNAPSHOT"
  :description "A simple to-do app in Clojure"
  :url "http://todo-clojure.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [ring "1.12.2"]
                 [ring/ring-defaults "0.4.0"]
                 [ring/ring-jetty-adapter "1.12.2"]
                 [compojure "1.7.1"]
                 [hiccup "1.0.5"]
                 [clojure.java-time "1.4.3"]]
  :main ^:skip-aot todo.core
  :profiles {:uberjar {:aot :all}})
