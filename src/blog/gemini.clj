(ns blog.gemini
  (:require
   [blog.time :as time]
   [blog.gemtext :as gemtext]))

(defn feed-page [posts]
  (gemtext/unparse
   (list
    [:header-1 "yumh"]
    [:quote "Writing about things, sometimes"]
    [:text ""]
    (for [post posts]
      (let [{:keys [title date slug]} post
            url (str "gemini://gemini.omarpolo.com/post/" slug ".gmi")]
        [:link url (str (time/fmt-iso8601 date) " - " title)])))))

(defn with-page [_ & body]
  (gemtext/unparse
   (list
    body
    [:text ""]
    [:text ""]
    [:text ""]
    [:text "-- text: CC0 1.0; code: public domain (unless specified otherwise)"]
    [:text "For comments, write at < blog at omarpolo dot com > or @op@bsd.network in the fediverse."]
    [:link "//git.omarpolo.com/blog/" "Capsule proudly assembled with Clojure"])))

(defn with-default-template [_ & body]
  (with-page {}
    [:header-1 "yumh"]
    [:quote "Writing about things, sometimes"]
    [:text ""]
    [:link "/" "Home"]
    [:link "/tags.gmi" "All Tags"]
    [:link "/pages/projects.gmi" "Projects"]
    [:text ""]
    body))

(defn post-fragment
  [{:keys [full? title-with-link?]}
   {:keys [title date slug tags short body toot music xkcd] :as post}]
  (list
   (if title-with-link?
     [:link (str "/post/" slug ".gmi") (str (time/fmt-iso8601 date) " - " title)]
     [(if full? :header-1 :header-2) title])
   (if full?
     [:text ""]
     [:quote short])
   (when music
     [:text (str "Written while listening to “" (:title music) "”"
                      (when-let [by (:by music)]
                        (str " by " by))
                      ".")])
   (when full?
     [:text (str "Published: " (time/fmt-iso8601 date))])
   [:text "Tagged with:"]
   (map #(vector :link (str "/tag/" (name %) ".gmi") (str "#" (name %)))
        (sort tags))
   (when xkcd
     [:link (str "https://xkcd.com/" xkcd) (format "Relevant XKCD – #%d" xkcd)])
   (when full?
     (list [:text ""]
           (gemtext/parse body)))
   [:text ""]))

(defn home-page [{:keys [posts has-next has-prev nth]}]
  (with-default-template
    [:text ""]
    [:text "Welcome to my gemlog!  Sometimes I remember that I have a blog and post something here.  My main interests are computer science, operating systems (BSDs in particular), programming languages (especially C, Go, LISP in its various incarnations).  I also have an Italian capsule where I write about more casual stuff:"]
    [:link "gemini://it.omarpolo.com" "l'angolo di yumh"]
    [:text ""]
    [:text "Some Gemini services on this capsule:"]
    [:link "/cgi/man"    "Look up a manpage"]
    [:link "/cgi/gempkg" "Browse the OpenBSD ports tree"]
    [:text ""]
    [:header-2 "Recent posts"]
    [:text ""]
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

(defn custom-page [{:keys [body]}]
  (apply with-default-template (gemtext/parse body)))

(defn post-page [{:keys [title short] :as post}]
  (with-page {}
    [:link ".." "↩ back to the index"]
    [:text ""]
    (post-fragment {:full? true}
                   post)))

(defn tags-page [tags]
  (with-default-template
    [:header-2 "All tags"]
    [:text ""]
    (map #(vector :link (str "/tag/" (name %) ".gmi") (str "#" (name %)))
         (sort (fn [a b]
                   (compare (.toLowerCase (name a))
                            (.toLowerCase (name b)))) tags))))

(defn tag-page [tag posts]
  (with-default-template
    [:header-2 (format "Posts tagged with #%s" tag)]
    [:text ""]
    [:text "Note: not every post is currently available over Gemini."]
    [:text ""]
    (map (partial post-fragment {:title-with-link? true})
         (->> posts
              (sort-by :date)
              (reverse)))))
