(ns blog.core
  (:require
   [blog.rss :as rss]
   [blog.http :as http]
   [blog.gemini :as gemini]
   [blog.time :as time]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]])
  (:import (java.io File))
  (:gen-class))

(defn copy-file [src dst]
  (with-open [in  (io/input-stream (io/file src))
              out (io/output-stream (io/file dst))]
    (io/copy in out)))

(defn post [{:keys [slug gemtext?] :as p}]
  (let [ext (if gemtext? ".gmi" ".md")]
    (-> p
        (assoc  :body (-> (str "posts/" slug ext) io/resource slurp))
        (update :date time/parse))))

(def pages (atom nil))
(def per-tag (atom {}))
(def posts (atom []))

(defn add-post! [m]
  (let [p (post m)]
    (swap! posts conj p)
    (doseq [t (:tags m)]
      (swap! per-tag update t conj p))))

(defn load-posts! []
  (reset! per-tag {})
  (reset! posts [])
  (doseq [p (-> "posts.edn"
                io/resource
                slurp
                edn/read-string)]
    (add-post! p)))

(defn load-pages! []
  (reset! pages (->> "pages.edn"
                     io/resource
                     slurp
                     edn/read-string
                     (map (fn [{:keys [slug] :as p}]
                            (assoc p :body (as-> slug $
                                                (str "pages/" $ ".gmi")
                                                (io/resource $)
                                                (slurp $))))))))

(defn create-dirs! []
  (doseq [d ["resources/out"
             "resources/out/gemini"
             "resources/out/gemini/pages"
             "resources/out/gemini/post"
             "resources/out/gemini/tag"
             "resources/out/gemini/img"
             "resources/out/http"
             "resources/out/http/css"
             "resources/out/http/pages"
             "resources/out/http/post"
             "resources/out/http/tag"
             "resources/out/http/img"]]
    (.. (File. d) mkdirs)))

(defn gemini-post [{? :gemtext?}] ?)

(defn post-pages [{:keys [proto]}]
  (let [tags (keys @per-tag)
        ext  (if (= proto :gemini) ".gmi" ".html")
        ffn  (if (= proto :gemini) gemini-post identity)]
    (map-indexed (fn [i posts]
                   {:filename (if (= i 0)
                                (str "index" ext)
                                (str (inc i) ext))
                    :tags tags
                    :nth (inc i)
                    :posts posts
                    :has-next true
                    :has-prev true})
                 (partition-all 6 (filter ffn @posts)))))

(defn fix-next-last
  "Fix the :has-prev/:has-next for the post pages.  This assumes
  that `(not (empty? post-pages))`"
  [post-pages]
  (-> post-pages
      (->> (into []))
      (update 0                        assoc :has-prev false)
      (update (dec (count post-pages)) assoc :has-next false)))

(defn render-pages [pagefn proto ext]
  (doseq [page @pages
          :let [{:keys [slug]} page
                filename (str "resources/out/"
                              (name proto) "/pages/"
                              slug ext)]]
    (spit filename
          (pagefn page))))

(defn render-post-list [viewfn proto]
  (doseq [p    (fix-next-last (post-pages {:proto proto}))
          :let [{:keys [filename]} p]]
    (spit (str "resources/out/" (name proto) "/" filename)
          (viewfn p))))

(defn render-post [viewfn proto ext {s :slug, :as post}]
  (spit (str "resources/out/" (name proto) "/post/" s ext)
        (viewfn post)))

(defn render-tags [viewfn proto ext tags]
  (spit (str "resources/out/" (name proto) "/tags" ext)
        (viewfn tags)))

(defn render-tag [viewfn proto ext tag posts]
  (spit (str "resources/out/" (name proto) "/tag/" tag ext)
        (viewfn tag posts)))

(defn render-rss []
  (spit (str "resources/out/gemini/rss.xml")
        (rss/feed #(str "gemini://gemini.omarpolo.com/post/" % ".gmi")
                  (->> @posts
                       (filter gemini-post)
                       (map #(dissoc % :body)))))
  (spit (str "resources/out/http/rss.xml")
        (rss/feed #(str "https://www.omarpolo.com/post/" % ".html") @posts)))

(defn copy-dir
  "Copy the content of resources/`dir` to resources/out/`proto`/`dir`, assuming
  these two directories exists.  It does not copy recursively."
  [dir proto]
  (let [in (io/file (str "resources/" dir "/"))
        out         (str "resources/out/" proto "/" dir "/")]
    (doseq [f (->> in file-seq (filter #(.isFile %)))]
      (io/copy f (io/file (str out (.getName f)))))))

(defn copy-assets
  "Copy css and images to their places"
  []
  (copy-dir "img" "http")
  (copy-dir "img" "gemini")
  (copy-file "resources/favicon.ico" "resources/out/http/favicon.ico")
  (copy-file "resources/css/style.css" "resources/out/http/css/style.css"))

(comment (build)
         (count (filter gemini-post @posts))
         (gemini/post-page (first @posts))
         )

(defn build
  "Build the blog"
  []
  (create-dirs!)
  (copy-assets)
  (render-rss)
  (doseq [[proto ffn ext homefn postfn tagsfn tagfn pagefn]
          [[:http identity ".html" http/home-page http/post-page http/tags-page http/tag-page http/custom-page]
           [:gemini gemini-post ".gmi" gemini/home-page gemini/post-page gemini/tags-page gemini/tag-page gemini/custom-page]]]
    (render-pages pagefn proto ext)
    (render-post-list homefn proto)
    (doseq [p (filter ffn @posts)]
      (render-post postfn proto ext p))
    (render-tags tagsfn proto ext (keys @per-tag))
    (doseq [t @per-tag
            :let [[tag posts] t]]
      (render-tag tagfn proto ext (name tag) (filter ffn posts)))))

(def j (atom nil))

(defn serve
  "Serve a preview"
  []
  (reset!
   j
   (jetty/run-jetty (-> (fn [_] {:status 404, :body "not found"})
                        (wrap-resource "out")
                        (wrap-content-type))
                    {:port 3030
                     :join? false})))

(defn clean
  "clean the output directory"
  []
  (sh "rm" "-rf" "resources/out/http/")
  (sh "rm" "-rf" "resources/out/gemini/"))

(defn local-deploy
  "Copy the files to the local server"
  []
  (sh "rsync" "-r" "--delete" "resources/out/http/" "/var/www/omarpolo.local/"))

(defn deploy
  "Copy the files to the server"
  []
  (sh "rsync" "-r" "--delete" "resources/out/http/"   "op:sites/www.omarpolo.com/")
  (sh "rsync" "-r" "--delete" "resources/out/gemini/" "op:gemini"))

(defn stop-jetty []
  (.stop @j)
  (reset! j nil))

(defn -main [& actions]
  (load-posts!)
  (load-pages!)
  (doseq [action actions]
    (case action
      "clean"  (clean)
      "build"  (build)
      "deploy" (deploy)

      (println "unrecognized action" action))))

(comment
  (do
    (load-posts!)
    (load-pages!)
    ;; (clean)
    (build)
    (local-deploy))
  (serve)
  (stop-jetty)

  (do
    (deploy)
    (local-deploy))
)
