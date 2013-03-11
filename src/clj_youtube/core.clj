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

(defn get-favorites-feed
  "Fetch the favorites for a user given by `token`. Or `user-name` Feed data is in :xml key of response if response status is 200 OK."
  ([developer-key token]
     ((authenticated-request developer-key token)
                            {:request-method :get
                             :url urls/current-user-favorites-url}))
  ([username]
     (public-request {:request-method :get
                      :url (urls/public-favorites-url username)})))

(defn authenticate
  [request developer-key token]
  ((-> identity
       (authsub/wrap-developer-key developer-key)
       (authsub/wrap-token token)
       authsub/wrap-host-fix
       authsub/wrap-gdata-version) request))


(defn paginate
  [request start-index max-results]
  (-> request
      (assoc-in [:query-params :max-results] max-results)
      (assoc-in [:query-params :start-index] start-index)))

(defn unchunk [s]
  (when (seq s)
    (lazy-seq
      (cons (first s)
            (unchunk (next s))))))

(defn paginate-seq
  ([request start-index page-size]
     (unchunk
      (map
       (fn [i]
         (paginate request (+ start-index (* i page-size)) page-size))
       (range))))
  ([request start-index]
     (paginate-seq request start-index 50))
  ([request]
     (paginate-seq request 0 50)))

(defn lazy-pages
  ([f request start-index]
     (take-while #(= 200 (:status %))
                 (map f (paginate-seq request start-index 50))))
  ([f request]
     (lazy-pages f request 1))
  ([request]
     (lazy-pages (-> http-core/request
                    http/wrap-query-params
                    parse/wrap-xml)
                 request 1)))

(defn lazy-feed
  [base-request]
  (mapcat identity (take-while seq (unchunk (map (comp parse/parse-feed :xml) (lazy-pages base-request))))))

(defn request-from-url
  ([method url]
     (assoc (http/parse-url url)
       :request-method method))
  ([url]
     (request-from-url :get url)))

(def fetcher
  (http/wrap-query-params http-core/request))

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
   {:request-method :post
    :headers {"Content-Type" "application/atom+xml; charset=UTF-8"}
    :url urls/get-upload-token-url
    :body post-data}))

(defn direct-upload-request
  [developer-key token xml-info mime-type filename video-data]
  ((authenticated-request developer-key token)
   {:request-method :post
    :headers {"Slug" filename}
    :url urls/direct-upload-url
    :multipart [{:mime-type "application/atom+xml"
                 :encoding  "UTF-8"
                 :name "info"
                 :content xml-info}
                {:mime-type mime-type
                 :name "video-upload.mp4"                 
                 :content video-data}]}))


