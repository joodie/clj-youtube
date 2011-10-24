(ns clj-youtube.core
  "Youtube feed retrieval functions"
  (:require [clj-http.client :as http]
            [clj-http.core :as http-core]
            [authsub.middleware :as authsub]
            [clj-youtube.urls :as urls]
            [clj-youtube.parse-feed :as parse]))

(def public-request
  (-> http-core/request
      parse/wrap-xml
      http/wrap-url))

(defn authenticated-request
  [developer-key token]
  (-> http-core/request
      parse/wrap-xml
      (authsub/wrap-developer-key developer-key)
      (authsub/wrap-token token)
      authsub/wrap-host-fix
      authsub/wrap-gdata-version
      http/wrap-url))


(defn get-upload-feed
  "Fetch the uploads for a channel given by `token`. Or `user-name` Feed data is in :xml key of response if response status is 200 OK."
  ([developer-key token]
     ((authenticated-request developer-key token)
                            {:request-method :get
                             :url urls/current-user-feed-url}))
  ([username]
     (public-request {:request-method :get
                      :url (urls/public-feed-url username)})))

(defn get-video-info
  "Fetch info for a single video."
  [code]
  (public-request {:request-method :get
                       :url (urls/video-info-url code)}))

#_"<?xml version=\"1.0\"?>
<entry xmlns=\"http://www.w3.org/2005/Atom\"
  xmlns:media=\"http://search.yahoo.com/mrss/\"
  xmlns:yt=\"http://gdata.youtube.com/schemas/2007\">
  <media:group>
    <media:title type=\"plain\">Bad Wedding Toast</media:title>
    <media:description type=\"plain\">
      I gave a bad toast at my friend's wedding.
    </media:description>
    <media:category
      scheme=\"http://gdata.youtube.com/schemas/2007/categories.cat\">People
    </media:category>
    <media:keywords>toast, wedding</media:keywords>
  </media:group>
</entry>"


(defn make-minimal-video-info
  [title description category & keywords]
  (with-out-str
    (clojure.xml/emit
     {:tag :entry, :attrs
                       {:xmlns "http://www.w3.org/2005/Atom",
                        :xmlns:media "http://search.yahoo.com/mrss/",
                        :xmlns:yt "http://gdata.youtube.com/schemas/2007"},
                       :content [{:tag :media:group, :attrs nil,
                                  :content [{:tag :media:title,
                                             :attrs {:type "plain"},
                                             :content [title]}
                                            {:tag :media:description,
                                             :attrs {:type "plain"},
                                             :content [description]}
                                            {:tag :media:category,
                                             :attrs {:scheme "http://gdata.youtube.com/schemas/2007/categories.cat"},
                                             :content [category]}
                                            {:tag :media:keywords, :attrs nil,
                                             :content [(apply str (interpose ", " keywords))]}]}]})))

(defn get-upload-token
  [developer-key token post-data]
  ((authenticated-request developer-key token)
   {:request-method :get
    :url urls/get-upload-token-url
    :body post-data}))


