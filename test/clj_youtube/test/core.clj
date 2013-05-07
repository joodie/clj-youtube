(ns clj-youtube.test.core
  (:use [clojure.test])
  (:require [clj-youtube.core :as youtube]
            [clj-youtube.parse-feed :as parse]))

(deftest test-get-public-feed
  (let [feed (parse/parse-feed (:xml (youtube/get-upload-feed "officialpsy")))]
    (is (seq feed))
    (let [entry (first feed)]
      (is (= "officialpsy" (:author entry)))
      (let [link (:link (first feed))]
       (is (.startsWith link "http"))))))

