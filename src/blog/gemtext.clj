(ns blog.gemtext
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]))

(defn- starts-with?
  "check if `s` starts with `substr`.  Return `false` if `s` is not a
  string."
  [s substr]
  (when (string? s)
    (str/starts-with? s substr)))

(defn- match-code-blocks []
  (fn [rf]
    (let [acc (volatile! [])
          state (volatile! :normal)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result line]
         (let [in-verbatim? (= :verbatim @state)
               marker? (starts-with? line "```")]
           (cond
             (and (not in-verbatim?) marker?) ;go to verbatim
             (do (vreset! state :verbatim)
                 result)

             (and in-verbatim? marker?) ;return what we've got and go to :normal
             (let [res [:verbatim (str/join "\n" @acc)]]
               (vreset! state :normal)
               (vreset! acc [])
               (rf result res))

             in-verbatim?
             (do (vswap! acc conj line)
                 result)

             :else
             (rf result line))))))))

(defn- match-headings []
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result line]
       (rf result
           (cond
             ;; the space character after the # is madatory
             (starts-with? line "# ")   [:h1 (subs line 2)]
             (starts-with? line "## ")  [:h2 (subs line 3)]
             (starts-with? line "### ") [:h3 (subs line 4)]
             :else                      line))))))

(defn- generic-matcher
  "Return a generic matcher transducer.  Will wrap line that starts with
  `start` within `[type line]`."
  [start type]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result line]
       (rf result
           (if (starts-with? line start)
             [type (subs line (count start))]
             line))))))

(defn- match-lists [] (generic-matcher "* " :li))
(defn- match-blockquotes [] (generic-matcher "> " :blockquote))

(defn- match-links []
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result line]
       (let [spaces? #{\space \tab}
             nonblank? (complement spaces?)]
         (rf result
             (if-not (starts-with? line "=>")
               line
               (->> (seq line)
                    (drop 2)               ; drop the marker
                    (drop-while spaces?)   ; drop also the optional spaces
                    (split-with nonblank?) ; separate URL from (optional) label
                    (apply #(vector :link
                                    (apply str %1)
                                    (apply str (drop-while spaces? %2))))))))))))

(defn match-paragraphs [] (generic-matcher "" :paragraph))

(def parser
  (comp (match-code-blocks)
        (match-headings)
        (match-lists)
        (match-blockquotes)
        (match-links)
        (match-paragraphs)))

(defn parse
  "Given a string representing a gemtext document, parse it into an
  hiccup-like data structure."
  [str]
  (transduce parser conj [] (str/split-lines str)))

(defn unparse [thing]
  (let [sw (StringBuilder.)]
    (walk/prewalk
     (fn [t]
       (cond
         (nil? t)    nil

         (or (seq? t)
             (vector? t))
         (if (keyword? (first t))
           (let [[type a b] t]
             (.append sw
                      (case type
                        :verbatim   (str "```\n" a "\n```")
                        :h1         (str "# " a)
                        :h2         (str "## " a)
                        :h3         (str "### " a)
                        :li         (str "* " a)
                        :blockquote (str "> " a)
                        :link       (str "=> " a " " b)
                        :paragraph  a))
             (.append sw "\n")
             nil)
           t)))
     thing)
    (.toString sw)))

(defn to-hiccup [doc]
  (let [l (atom [])]
    (walk/prewalk
     (fn [t]
       (cond
         (nil? t) nil

         (or (seq? t)
             (vector? t))
         (if (keyword? (first t))
           (let [[type a b] t]
             (swap! l conj
                    (case type
                      :verbatim   [:pre [:code a]]
                      :h1         [:h1 a]
                      :h2         [:h2 a]
                      :h3         [:h3 a]
                      :li         [:ul [:li a]] ;; TODO!
                      :blockquote [:blockquote a]
                      :link       [:p.link [:a {:href a} b]]
                      :paragraph  [:p a]))
             nil)
           t)))
     doc)
    (seq @l)))

(comment

  (unparse (parse "```\nhello there\n```\n"))

  (unparse (list [:h2 "hello there"] (list (list nil (list nil)))))
  (unparse (list [:h2 "hello there"]))

  )
