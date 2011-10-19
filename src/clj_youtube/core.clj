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



