(ns blog.net-gemini
  (:import (com.omarpolo.gemini Request)))

(defn head [host port req]
  (with-open [res (Request. host port (str req "\r\n"))]
    {:code (.getCode res)
     :meta (.getMeta res)}))

(comment
  (with-open [res (Request. "gemini://localhost/index.gmi")]
    {:code (.getCode res)
     :meta (.getMeta res)})
)
