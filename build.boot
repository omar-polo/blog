(set-env!
 :resource-paths #{"src" "resources"}
 :dependencies '[[hiccup "1.0.5"]
                 [ring "1.8.0"]
                 [commonmark-hiccup "0.1.0"]
                 [org.clojure/data.xml "0.2.0-alpha6"]])

(task-options!
 pom {:project 'blog
      :version "0.1.0"})

(require '[blog.core :refer :all])
