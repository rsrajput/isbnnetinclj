(ns isbnnetinclj.views.book
  (:require [timbre.core :as log]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [noir.core :refer :all]
            [noir.request]
            [noir.response]
            [stencil.core :as mus]
            [monger.collection :as mc]
            [isbnnetinclj.models.constants :refer :all]
            [isbnnetinclj.models.info :refer [book-info]]
            [isbnnetinclj.models.stores :refer [book-data sites kindle-page]]
            [isbnnetinclj.utils :as utils]))


(defn core-details-of-request
  "NOTE This function should change if deployed outside Heroku"
  [request]
  {:ip (get-in request [:headers "x-forwarded-for"])
   :user-agent (get-in request [:headers "user-agent"])
   :isbn (string/replace (:uri request) "/" "")
   :when (java.util.Date.)})


(defn convert-prices-for-display
  [isbn prices]
  (map #(assoc % :price (if (= (Integer/MAX_VALUE) (:price %)) "not available" (:price %)))
       (sort-by :price (map (fn [[store-name price]]
                              {:name (name store-name)
                               :price price
                               :url (format (get-in sites [store-name :url]) isbn)})
                            prices))))


(defn is-isbn-10
  [isbn]
  (re-matches #"^[0-9]{9}[0-9xX]$" isbn))


(defn is-isbn-13
  [isbn]
  (re-matches #"^[0-9]{13}$" isbn))


;; http://refactormycode.com/codes/33-isbn10-to-isbn13#refactor_257
(defn convert-isbn-10-to-13
  ;; http://clojuredocs.org/clojure_core/clojure.core/test
  {:test #(assert (= "9780142000281" (convert-isbn-10-to-13 "0142000280")))}
  [isbn]
  (let [x (map (comp #(Integer/parseInt %) str) isbn)
        sum-of-digits (+ 38
                         (* 3 (+ (nth x 0) (nth x 2) (nth x 4) (nth x 6) (nth x 8)))
                         (+ (nth x 1) (nth x 3) (nth x 5) (nth x 7)))
        check-digit (mod (- 10 (mod sum-of-digits 10)) 10)
        isbn-except-last-digit (.substring isbn 0 (- (.length isbn) 1))]
    (str "978" isbn-except-last-digit check-digit)))


(defn strip-dashes
  [text]
  (string/replace text "-" ""))


(defpage [:get ["/:isbn" :isbn #"[\d-]+[xX]?"]] {:keys [isbn]}
  (let [isbn (strip-dashes isbn)]
    (if (is-isbn-10 isbn)
      (noir.response/redirect (str "/" (convert-isbn-10-to-13 isbn)) :permanent)
      (if (is-isbn-13 isbn)
        (let [data (book-data isbn) ; NOTE Launching price fetchers in background before starting info fetcher
              info (book-info isbn)]
          (mc/insert request-collection (core-details-of-request (noir.request/ring-request)))
          (mus/render-file "book" {:prices (convert-prices-for-display isbn (:price data))
                                   :isbn isbn
                                   :info (:info info)
                                   :title (or (get-in info [:info :title]) "isbn.net.in")
                                   :kindle-page (if (boolean (not-empty (keys (:price data)))) (kindle-page isbn))}))))))


(defn prepare-data-for-json
  [isbn {:keys [_id when price]}]
  (log/debug "ISBN is" isbn)
  {:isbn isbn
   :when (str when)
   :price price})


(defpage [:get ["/:isbn.json" :isbn #"[\d-]+[xX]?"]] {:keys [isbn]}
  (let [isbn (strip-dashes isbn)]
    (if (is-isbn-10 isbn)
      (noir.response/redirect (str "/" (convert-isbn-10-to-13 isbn) ".json") :permanent)
      (if (is-isbn-13 isbn)
        (let [data (book-data isbn)]
          (mc/insert request-collection (core-details-of-request (noir.request/ring-request)))
          (noir.response/json (prepare-data-for-json isbn data)))))))
