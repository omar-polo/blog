(ns blog.gemini
  (:require
   [blog.time :as time]
   [blog.gemtext :as gemtext]))

(defn with-page [_ & body]
  (gemtext/unparse
   (list
    [:verbatim
"                       _
 _   _ _   _ _ __ ___ | |__
| | | | | | | '_ ` _ \\| '_ \\
| |_| | |_| | | | | | | | | |
 \\__, |\\__,_|_| |_| |_|_| |_|
 |___/"]
    [:paragraph "Writing about things, sometimes."]
    [:paragraph ""]
    [:link "/" "Home"]
    [:link "/tags.gmi" "All Tags"]
    [:link "https://git.omarpolo.com" "Git repos"]
    [:paragraph ""]
    body
    [:paragraph ""]
    [:paragraph ""]
    [:paragraph "Blog proudly generated with Clojure"]
    [:link "https://git.omarpolo.com/blog/" "sources"])))

(defn post-fragment
  [{:keys [full? title-with-link?]}
   {:keys [title date slug tags short body toot music xkcd] :as post}]
  (list
   (if title-with-link?
     [:link (str "/post/" slug ".gmi") title]
     [(if full? :h1 :h2) title])
   (when full?
     [:paragraph ""])
   [:paragraph (str "Written by Omar Polo on " (time/fmt-loc date)
                    (when music
                      (str " while listening to " (:title music) (when-let [by (:by music)]
                                                                   (str " by " by)) ))
                    ".")]
   [:paragraph "Tagged with:"]
   (map #(vector :link (str "/tag/" (name %) ".gmi") (str "#" (name %)))
        tags)
   (when xkcd
     [:link (str "https://xkcd.com/" xkcd) (format "Relevant XKCD – #%d" xkcd)])
   (if full?
     (list [:paragraph ""]
           (gemtext/parse body))
     (when short [:blockquote short]))
   [:paragraph ""]))

(defn home-page [{:keys [posts has-next has-prev nth]}]
  (with-page {}
    [:h2 "Recent posts"]
    [:paragraph ""]
    (map (partial post-fragment {:title-with-link? true})
         posts)
    (when has-prev
      [:link (str "/"
                  (if (= (dec nth) 1)
                    "index"
                    (dec nth))
                  ".gmi")
       "Newer Posts"])
    (when has-next
      [:link (str "/" (inc nth) ".gmi")
       "Older Posts"])))

(defn post-page [{:keys [title short] :as post}]
  (with-page {}
    (post-fragment {:full? true}
                   post)))

(defn tags-page [tags]
  (with-page {}
    [:h2 "All tags"]
    [:paragraph ""]
    (map #(vector :link (str "/tag/" (name %) ".gmi") (str "#" (name %)))
         (sort (fn [a b]
                   (compare (.toLowerCase (name a))
                            (.toLowerCase (name b)))) tags))))

(defn tag-page [tag posts]
  (with-page {}
    [:h2 (format "Posts tagged with #%s" tag)]
    [:paragraph ""]
    (map (partial post-fragment {:title-with-link? true})
         (->> posts
              (sort-by :date)
              (reverse)))))