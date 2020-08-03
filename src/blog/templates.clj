(ns blog.templates
  (:require [blog.time :as time]
            [hiccup.page :refer [html5 include-css]]
            [commonmark-hiccup.core :refer [markdown->hiccup default-config]]))

(defn link-item [{:keys [url text]}]
  [:li [:a {:href url} text]])

(defn header [{:keys [tags]}]
  (list
   [:header
    [:nav
     [:ul
      (link-item {:url "/", :text "Home"})
      (link-item {:url "/tags.html", :text "All Tags"})
      (link-item {:url "https://git.omarpolo.com", :text "Git repos"})]]
    [:div
     [:h1 [:a {:href "/"} "yumh"]]
     [:p "writing about things, sometimes."]]]))

(defn with-page
  [{:keys [title class description], :as d} & body]
  (html5 {:lang "en"}
   [:head
    [:meta {:charset "utf8"}]
    [:meta {:name "viewport", :content "width=device-width, initial-scale=1"}]
    [:link {:rel "shortcut icon", :href "/favicon.ico"}]
    [:link {:rel "alternative" :type "application/rss+xml" :href "https://www.omarpolo.com/rss.xml"}]
    (when description
      [:meta {:name "description" :content description}])
    [:title title]
    (include-css "/css/style.css")]
   [:body {:class (or class "")}
    (header d)
    [:main body]
    [:footer
     [:p "Blog proudly generated with "
      [:a {:href "https://git.omarpolo.com/blog/"}
       [:code "(clojure)"]]]]
    [:noscript
     [:img {:src "https://goatcounter.omarpolo.com/count?p=/test-img"}]]
    [:script {:data-goatcounter "https://goatcounter.omarpolo.com/count"
              :async true
              :src "//goatcounter.omarpolo.com/count.js"}]]))

(defn post-fragment
  [{:keys [full? title-with-link?]}
   {:keys [title date slug tags short body toot music], :as post}]
  [:article
   [:header
    [(if full?
       :h1
       :h2.fragment)
     (if title-with-link?
       [:a {:href (str "/post/" slug ".html")} title]
       title)]
    [:p.author "Written by " [:em "Omar Polo"] " on " (time/fmt-loc date)
     (if music
       (list " while listening to " [:a {:href   (:url music)
                                         :target "_blank"
                                         :rel    "noopener"}
                                     "“" [:em (:title music)] "”" " by " [:em (:by music)]] ".")
       ".")]
    [:ul.tags (map #(vector :li [:a {:href (str "/tag/" (name %) ".html")}
                                 (str "#" (name %))])
                   tags)]
    (when toot
      [:p [:a {:href   toot,
               :target "_blank"
               :rel    "noopener"} "Comments over ActivityPub"]])]
   [:section
    (if full?
      (markdown->hiccup default-config body)
      [:p short])]])

(defn home-page
  [{:keys [posts has-next has-prev nth]}]
  (with-page {:title "Home"}
    (map (partial post-fragment {:title-with-link? true})
         posts)
    [:nav.post-navigation
     (if has-prev
       [:a.prev {:href (str "/" (if (= (dec nth) 1)
                                  "index"
                                  (dec nth)) ".html")}
        "« Newer Posts"])
     (if has-next
       [:a.next {:href (str "/" (inc nth) ".html")}
        "Older Posts »"])]))

(defn post-page
  [{:keys [title short], :as post}]
  (with-page {:title title
              :class "article"
              :description short}
    (post-fragment {:full? true}
                   post)))

(defn tags-page
  [tags]
  (with-page {:title "All tags"
              :class "tags"}
    [:h2 "All tags"]
    [:nav
     [:ul
      (map #(vector :li [:a {:href (str "/tag/" (name %) ".html")} (str "#" (name %))])
           (sort (fn [a b]
                   (compare (.toLowerCase (name a))
                            (.toLowerCase (name b)))) tags))]]))

(defn tag-page
  [tag posts]
  (with-page {:title (str "Posts tagged with #" tag)
              :class "tag"}
    [:h2 "Posts tagged with " [:code "#" tag]]
    (map (partial post-fragment {:title-with-link? true})
         (->> posts
              (sort-by :date)
              (reverse)))))
