(ns blog.rss
  (:require [blog.time :as time]
            [clojure.data.xml :refer :all]
            [hiccup.core :as hiccup]
            [commonmark-hiccup.core :refer [markdown->hiccup default-config]]))

(defn item [{:keys [title date slug tags short body]}]
  (let [link (str "https://www.omarpolo.com/post/" slug ".html")]
    [:item
     [:title title]
     [:description
      [:-cdata
       (hiccup/html
        (markdown->hiccup default-config body))]]
     [:guid link]
     [:link link]
     [:pubDate (time/fmt-rfc-2822 date)]]))

(defn feed [posts]
  (indent-str
   (sexp-as-element
    [:rss {:version "2.0" :xmlns:atom "http://www.w3.org/2005/Atom"}
     [:channel
      [:title "yumh"]
      [:description "Writing about things, sometimes"]
      [:link "https://www.omarpolo.com"]
      ;; fails to validate?
      ;; [:atom:link {:href "https://www.omarpolo.com/rss.xml" :ref "self" :type "application/rss+xml"} nil]
      (map item posts)]])))
