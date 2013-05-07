(ns clj-youtube.test.core
  (:use [clojure.test])
  (:require [clj-youtube.core :as youtube]
            [clj-youtube.parse-feed :as parse]))

(deftest test-get-public-feed
  (let [feed (parse/parse-feed (:xml (youtube/get-upload-feed "zeekatcode")))]
    (is (seq feed))
    (let [entry (first feed)]
      (is (= "zeekatcode" (:author entry)))
      (let [link (:link (first feed))]
        (is (.startsWith link "http"))))))

(def next-page-xml-href
  "NEXT_PAGE_TARGET")

(def next-page-xml-fragment
  {:tag :feed,
   :attrs
   {:xmlns "http://www.w3.org/2005/Atom",
    :xmlns:openSearch "http://a9.com/-/spec/opensearchrss/1.0/",
    :xmlns:yt "http://gdata.youtube.com/schemas/2007"},
   :content
   [{:tag :id,
     :attrs nil,
     :content
     ["http://gdata.youtube.com/feeds/api/videos/uQyLngbb7Ms/comments"]}
    {:tag :updated, :attrs nil, :content ["2013-05-07T21:11:39.034Z"]}
    {:tag :link,
     :attrs
     {:rel "next",
      :type "application/atom+xml",
      :href
      next-page-xml-href},
     :content nil}]})

(deftest test-next-page-url
  (is (= next-page-xml-href
         (parse/next-page-url next-page-xml-fragment))))
