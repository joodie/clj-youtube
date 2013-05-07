(ns clj-youtube.parse-feed
  "Minimal feed parsing routines"
  (:require [clojure.xml :as xml])
  (:import java.io.ByteArrayInputStream))

(defn to-xml
  "Make a clojure.xml datastructure given a string or a ByteArray"
  [content]
  (xml/parse (ByteArrayInputStream.
              (if (string? content)
                (.getBytes content)
                content))))

(defn xml-type?
  "true if s is an xml content type."
  [s]
  (and (string? s)
       (or (.startsWith s "text/xml")
           (.startsWith s "application/atom+xml"))))

(defn wrap-xml
  "parse response body using clojure.xml if status is OK.
XML feed, if available, is in (:xml response)"
  [f]
  (fn [& args]
    (let [resp (apply f args)]      
      (if (xml-type? (get (:headers resp) "content-type"))
        (assoc resp :xml (to-xml (:body resp)))
        resp))))

(defn get-tags
  [s key]
  (filter #(= key (:tag %)) s))

(defn get-tag
  [s key]
  (first (get-tags s key)))

(defn get-text
  [tag]
  (apply str (:content tag)))

(defn parse-entry
  "Get some basic info from a youtube entry XML element"
  [entry]
  {:title (-> entry
              :content
              (get-tag :title)
              get-text)
   :author (-> entry
               :content
               (get-tag :author)
               :content
               (get-tag :name)
               get-text)
   :description (-> entry
                    :content
                    (get-tag :content)
                    get-text)
   :thumbnail (-> entry
                  :content
                  (get-tag :media:group)
                  :content
                  (get-tag :media:thumbnail)
                  :attrs
                  :url)

   :comments (-> entry
                 :content
                 (get-tag :gd:comments)
                 :content
                 (get-tag :gd:feedLink)
                 :attrs
                 :href)
   
   :link (-> (first (filter #(and (= "text/html" (-> % :attrs :type))
                                  (= "alternate" (-> % :attrs :rel)))
                            (-> entry
                                :content
                                (get-tags :link))))
             :attrs
             :href)

   :published (-> entry :content (get-tag :published) get-text)
   :updated (-> entry :content (get-tag :updated) get-text)})

(defn parse-feed
  "give a seq of video uploads from the feed content"
  [xml]
  (map parse-entry
       (-> xml
           :content
           (get-tags :entry))))

(defn parse-upload-token
  [xml]
  {:url (-> xml
            :content
            (get-tag :url)
            get-text)
   :token (-> xml
              :content
              (get-tag :token)
              get-text)})
