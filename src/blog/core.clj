(ns blog.core
  (:require [blog.time :as time]
            [blog.templates :as templates]
            [boot.core :as core]
            [boot.task.built-in :as task]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]])
  (:import (java.io File)))

(defn copy-file [src dst]
  (with-open [in  (io/input-stream (io/file src))
              out (io/output-stream (io/file dst))]
    (io/copy in out)))

(defn post [{:keys [slug title short date tags]}]
  (let [f (io/resource (str "posts/" slug ".md"))]
    {:slug slug
     :title title
     :short short
     :date (time/parse date)
     :body (slurp f)
     :tags tags}))

(def per-tag (atom {}))
(def posts (atom []))

(defn add-post! [m]
  (let [p (post m)]
    (swap! posts conj p)
    (doseq [t (:tags m)]
      (swap! per-tag update t conj p))))

(load "posts")

(defn create-dirs! []
  (doseq [d ["resources/out"
             "resources/out/css"
             "resources/out/post"
             "resources/out/tag"
             "resources/out/img"]]
    (.. (File. d) mkdirs)))

(defn post-pages []
  (let [tags (keys @per-tag)]
    (map-indexed (fn [i posts]
                   {:filename (if (= i 0)
                                "index.html"
                                (str (inc i) ".html"))
                    :tags tags
                    :nth (inc i)
                    :posts posts
                    :has-next true
                    :has-prev true})
                 (partition-all 6 @posts))))

(defn fix-next-last
  "Fix the :has-prev/:has-next for the post pages.  This assumes
  that `(not (empty? post-pages))`"
  [post-pages]
  (-> post-pages
      (->> (into []))
      (update 0                        assoc :has-prev false)
      (update (dec (count post-pages)) assoc :has-next false)))

(defn render-post-list []
  (doseq [p (fix-next-last (post-pages))
          :let [{:keys [filename]} p]]
    (spit (str "resources/out/" filename)
          (templates/home-page p))))

(defn render-post [{s :slug, :as post}]
  (spit (str "resources/out/post/" s ".html")
        (templates/post-page post)))

(defn render-tags [tags]
  (spit (str "resources/out/tags.html")
        (templates/tags-page tags)))

(defn render-tag [tag posts]
  (spit (str "resources/out/tag/" tag ".html")
        (templates/tag-page tag posts)))

(defn copy-dir
  "Copy the content of resources/`dir` to resources/out/`dir`, assuming
  these two directories exists.  It does not copy recursively."
  [dir]
  (let [in (io/file (str "resources/" dir "/"))
        out         (str "resources/out/" dir "/")]
    (doseq [f (->> in file-seq (filter #(.isFile %)))]
      (io/copy f (io/file (str out (.getName f)))))))

(comment
  (copy-dir "img")
  (io/copy (io/file "resources/img/unbound-dashboard.png")
           (io/file "resources/out/img/unbound-dashboard.png"))
)

(defn copy-assets
  "Copy css and images to their places"
  []
  (copy-dir "img")
  (copy-file "resources/favicon.ico" "resources/out/favicon.ico")
  (copy-file "resources/css/style.css" "resources/out/css/style.css"))

(core/deftask build
  "Build the blog"
  []
  (create-dirs!)
  (copy-assets)
  (render-post-list)
  (doseq [p @posts]
    (render-post p))
  (render-tags (keys @per-tag))
  (doseq [t @per-tag
          :let [[tag posts] t]]
    (render-tag (name tag) posts)))

(def j (atom nil))

(core/deftask serve
  "Serve a preview"
  []
  (reset!
   j
   (jetty/run-jetty (-> (fn [_] {:status 404, :body "not found"})
                        (wrap-resource "out")
                        (wrap-content-type))
                    {:port 3000
                     :join? false})))

(core/deftask clean
  "clean the output directory"
  []
  (sh "rm" "-rm" "resources/out/"))

(core/deftask deploy
  "Copy the files to the server"
  []
  (sh "openrsync" "-r" "--delete" "resources/out/" "op:sites/www.omarpolo.com/"))

(defn stop-jetty []
  (.stop @j)
  (reset! j nil))

(comment
  (build)
  (serve)
  (stop-jetty)
)
