(ns blog.net-gemini
  (:import (com.omarpolo.gemini Response)))

(defn head [host port req]
  (with-open [res (Response. host port (str req "\r\n"))]
    {:code (.getCode res)
     :meta (.getMeta res)}))
