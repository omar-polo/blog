(ns blog.net-gemini
  (:require [gemini.core :as gemini]))

(def antenna-uri "gemini://warmedal.se/~antenna")

(defn ping-antenna
  "Sends the given `url` to antenna."
  [url]
  (gemini/with-request [req {:request (str antenna-uri "/submit?" url)
                             :follow-redirects? true}]
    (gemini/body-as-string! req)))

(comment
  (ping-antenna "gemini://gemini.omarpolo.com")
)
