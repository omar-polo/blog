(ns blog.time
  (:import (java.time.format DateTimeFormatter FormatStyle)
           (java.time LocalDate)
           (java.util Locale)))

(def pattern
  (DateTimeFormatter/ofPattern "yyyy/MM/dd"))

(def loc (Locale/forLanguageTag "en"))

(def pattern-loc
  (DateTimeFormatter/ofPattern "dd MMMM yyy" loc))

(defn fmt [d]
  (.format d pattern))

(defn fmt-loc [d]
  (.format d pattern-loc))

(defn parse [s]
  (LocalDate/parse s pattern))

(comment
  (parse "2020/03/24")
  (fmt (LocalDate/now))
  (fmt-loc (LocalDate/now))
  )
