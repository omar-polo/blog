(ns blog.time
  (:import (java.time.format DateTimeFormatter FormatStyle)
           (java.time LocalDate)
           (java.util Locale Date)))

(def pattern
  (DateTimeFormatter/ofPattern "yyyy/MM/dd"))

(def loc (Locale/forLanguageTag "en"))

(def pattern-loc
  (DateTimeFormatter/ofPattern "dd MMMM yyy" loc))

(defn fmt [d]
  (.format d pattern))

(defn fmt-loc [d]
  (.format d pattern-loc))

(defn fmt-rfc-2822 [d]
  (let [pattern (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy")]
    (str (.format d pattern)
         " 00:00:00 GMT")))

(defn fmt-iso8601 [d]
  ;; quoted to indicate UTC, not TZ offset. Also, HH:mm is 00:00
  ;; because i'm parsing the raw date as LocalDate and not datetime,
  ;; so there isn't the info about the hour and the minute.
  (let [pattern (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'00:00'Z'")]
    (.format d pattern)))

(defn parse [s]
  (LocalDate/parse s pattern))

(comment
  (parse "2020/03/24")
  (fmt (LocalDate/now))
  (fmt-loc (LocalDate/now))
  )
