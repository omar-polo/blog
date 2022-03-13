(ns blog.gemtext
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [gemtext.core :as gemtext]))

(defn parse
  "Given a string representing a gemtext document, parse it into an
  hiccup-like data structure."
  [str]
  (gemtext/parse str))

(defn unparse [thing]
  (gemtext/unparse thing))

(defn maybe-patch-link [[type attrs body :as t]]
  (cond (not= type :a) t

        (re-matches #".*\.(jpg|jpeg|png|gif)" (:href attrs))
        (let [{:keys [href]} attrs]
          [:figure
           [:a {:href href}
            [:img {:src href
                   :alt body}]]
           [:figcaption body]])

        (and (re-matches #".*\.gmi" (:href attrs))
             (not (re-matches #"^gemini://.*" (:href attrs))))
        [:p.link [:a {:href (str/replace (:href attrs)
                                         #"\.gmi$"
                                         ".html")}
                  body]]

        :else
        [:p.link [:a {:href (:href attrs)}
                  body]]))

(defn not-empty-ps [[type body :as t]]
  (not
   (and (= type :p)
        (= body ""))))

(defn id-from-title [title]
  (-> title
      (str/replace #" +" "-")
      .toLowerCase))

(defn fix-headlines-id [[type body :as t]]
  (case type
    :h1 [:h1 {:id (id-from-title body)} body]
    :h2 [:h2 {:id (id-from-title body)} body]
    :h3 [:h3 {:id (id-from-title body)} body]
    t))

(defn to-hiccup [doc]
  (->> (gemtext/to-hiccup doc)
       (filter not-empty-ps)
       (map maybe-patch-link)
       (map fix-headlines-id)))

(comment
  (to-hiccup [[:link "http://f.com" "hello"]])
)
