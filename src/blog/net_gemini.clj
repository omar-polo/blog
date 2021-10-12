(ns blog.net-gemini
  (:import (com.omarpolo.gemini Gemini)))

(defn head [host port req]
  (with-open [res (Gemini/get host port (str req "\r\n"))]
    {:code (.getCode res)
     :meta (.getMeta res)}))
