(ns blog.http
  (:require
   [blog.time :as time]
   [blog.gemtext :as gemtext]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [hiccup.page :refer [html5 include-css]]
   [commonmark-hiccup.core :refer [markdown->hiccup default-config]]))

(defn link-item [{:keys [url text title]}]
  [:li [:a (cond-> {:href url}
             title (assoc :title title))
        text]])

(defn header [{:keys [skip-banner?]}]
  (list
   [:header
    [:nav
     [:ul
      (link-item {:url "/", :text "Home"})
      (link-item {:url "/tags.html", :text "All Tags"})
      (link-item {:url "/pages/projects.html", :text "Projects"})
      #_(link-item {:url "/dots", :text "dotfiles"})
      (link-item {:url "gemini://gemini.omarpolo.com" :text "gemini://"
                  :title "This website in the gemini space."})]]
    (when-not skip-banner?
      [:div
       [:h1 [:a {:href "/"} "yumh"]]
       [:p "writing about things, sometimes."]])]))

(defn with-page
  [{:keys [title class description skip-banner?], :as d} & body]
  (html5 {:lang "en"}
   [:head
    [:meta {:charset "utf8"}]
    [:meta {:name "viewport", :content "width=device-width, initial-scale=1"}]
    [:meta {:http-equiv "Onion-Location" :content "http://n556bsewix6wn6ntegcuo74543nfbl25gfbzrcwvv5w52367t4csy3ad.onion"}]
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
     [:p "text: CC0 1.0; code: public domain (unless specified otherwise).  No copyright here."]
     [:p "Blog proudly generated with "
      [:a {:href "https://git.omarpolo.com/blog/"}
       [:code "(clojure)"]]]]
    (comment
      [:noscript
       [:img {:src "https://goatcounter.omarpolo.com/count?p=/test-img"}]]
      [:script "
  ;(function () {
    if (window.location.host !== 'www.omarpolo.com')
      window.goatcounter = {no_onload: true}
  })();"]
      [:script {:data-goatcounter "https://goatcounter.omarpolo.com/count"
                :async true
                :src "//goatcounter.omarpolo.com/count.js"}])]))

(defn link->images
  "traverse `doc` and replace every link to an image to an `img` tag."
  [doc]
  (walk/prewalk
   (fn [item]
     (if-not (and (vector? item) (= (first item) :a))
       item
       (let [[_ {:keys [href] :as attrs} text] item]
         [:p
          [:a attrs
           [:img {:src href
                  :alt text}]]])))))

(defn post-fragment
  [{:keys [full? title-with-link?]}
   {:keys [title date slug tags short body toot music xkcd gemtext?], :as post}]
  [:article
   [:header
    [(if full?
       :h1
       :h2.fragment)
     (if title-with-link?
       [:a {:href (str "/post/" slug ".html")} title]
       title)]
    [:p.author "Written by " [:em "Omar Polo"] " on " (time/fmt-loc date)
     (list
      (when music
        (let [b (list "“" [:em (:title music)] "”"
                      (when-let [by (:by music)]
                        (list " by " [:em by])))]
          (list " while listening to "
                (if-let [url (:url music)]
                  [:a {:href url
                       :target "_blank"
                       :rel    "noopener"}
                   b]
                  [:span b]))))
      ".")]
    [:ul.tags (map #(vector :li [:a {:href (str "/tag/" (name %) ".html")}
                                 (str "#" (name %))])
                   (sort tags))]
    (when xkcd
      [:p [:a {:href (str "https://xkcd.com/" xkcd)
               :target "_blank"
               :rel "noopener"}
           "Related XKCD"]])
    (when toot
      [:p [:a {:href   toot,
               :target "_blank"
               :rel    "noopener"} "Comments over ActivityPub"]])]
   [:section
    (if full?
      (if gemtext?
        (-> body gemtext/parse gemtext/to-hiccup)
        (markdown->hiccup default-config body))
      [:p short])]])

(defn home-page
  [{:keys [posts has-next has-prev nth]}]
  (with-page {:title "Home"}
    [:p "Hello!  Sometimes I remember that I have a blog and post something here.  "
     "You can find me " [:strike "wasting time"]
     " posting interesting stuff on the fediverse too: "
     ;; <a rel="me" href="https://bsd.network/@op">Mastodon</a>
     [:a {:href "https://bsd.network/@op"
          :rel "me"}
      "@op@bsd.network"] "."]
    [:p "I also have an Italian blog where I write about more casual stuff: "
     [:a {:href "https://it.omarpolo.com"}
      "https://it.omarpolo.com"]]
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

(defn custom-page [{:keys [title body]}]
  (with-page {:title        title
              :skip-banner? true}
    ;; warning: hack ahead
    (walk/prewalk
     (fn [item]
       (if-not (and (vector? item) (= (first item) :a))
         item
         (let [[_ attrs & body] item]
           [:a (update attrs :href str/replace #"\.gmi$" ".html")
            body])))
     (-> body gemtext/parse gemtext/to-hiccup))))

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
