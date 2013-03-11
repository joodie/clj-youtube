(ns clj-youtube.urls
  "Feed endpoints and url manipulation")

(def current-user-feed-url
  "http://gdata.youtube.com/feeds/api/users/default/uploads")

(defn public-feed-url
  [name]
  (format "http://gdata.youtube.com/feeds/api/users/%s/uploads" name))

(def current-user-favorites-url
  "http://gdata.youtube.com/feeds/api/users/default/favorites")

(defn public-favorites-url
  [name]
  (format "http://gdata.youtube.com/feeds/api/users/%s/favorites" name))

(def get-upload-token-url
  "http://gdata.youtube.com/action/GetUploadToken")

(def direct-upload-url
  "http://uploads.gdata.youtube.com/feeds/api/users/default/uploads")

(defn strip-youtube-hostname
  "Return path for youtube url - drops the host and http(s)
start. retuns nil if url isn't on youtube."
  [url]
  (second (re-matches #"^https?://(?:[^/]+\.)?(?:youtube(?:\.[^/]+)?(?:\.[^/]+)|youtu\.be)(?:[0-9]+)?(/.+)" url)))

(defn path-to-code [path]
  (or (second (re-matches #".*v=([^&]+).*" path))
      (second (re-matches #"/user/[^#]+#.*/([^/]+)$" path))))

(defn url-to-code
  "Returns the youtube video code given a youtube url or nil"
  [url]
  (or (second (re-matches #"^https?://youtu.be/(.+)" url))
      (if-let [path (strip-youtube-hostname url)]
        (path-to-code path))))

(defn to-code
  "Given a code or youtube url, give the video code or throw."
  [code-or-url]
  {:pre [(string? code-or-url)]}
  (cond
   (.startsWith code-or-url "http")
   (url-to-code code-or-url)
   
   (re-matches #"^[a-zA-Z0-9\-_]+$" code-or-url)
   code-or-url))

(defn embed-url
  "Url for iframe-based embedding given a video url or code"
  [video]
  (str "http://www.youtube.com/embed/" (to-code video)))

(defn video-info-url
  "Youtube video stream url for a single video. Expects a youtube url or video code"
  [video]
  (str "http://gdata.youtube.com/feeds/api/videos/" (to-code video)))

