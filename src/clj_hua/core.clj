(ns clj-hua.core
  (:gen-class)
  (:require
   [taoensso.timbre :as t]
   [sparrows.misc :refer [str->num]]
   [hiccup.core :refer :all]
   [hickory.core :refer [parse as-hickory]]
   [hickory.select                  :as hs]
   [clojure.string :as s :refer [join lower-case split starts-with?]]
   [clojure.java.shell :refer [sh]]
   [clojure.java.io :refer [file]]
   [clojure.set :refer [union difference]]
   [com.climate.claypoole  :as cp]
   [org.httpkit.client :as client]
   [hickory.utils :as u]
   [clojure.data.codec.base64 :as b64]
   [cheshire.core :as c]
   [clojure.java.io :as io]
   [clojure.java.jdbc :as j]))

(defonce base-url-store (atom "http://223.100.155.234:8013"))

(defn uri-to-url [uri]
  (str @base-url-store
       (when-not (starts-with? uri "/")
         "/")
       uri))

(def ^:private client-options
  {:timeout          10000
   :follow-redirects true
   :insecure?        true
   :headers          {"accept-encoding" "gzip"
                      "Host"            "223.100.155.234:8013"
                      "user-agent"      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.157 Safari/537.36"}})

(defn- web-get [url]
  (client/get url client-options))



(defn- parse-item [node]
  (let [title (some->> (hs/select (hs/class "txt") node) first :content first)
        book-url (some->> (hs/select (hs/class :img) node) first :attrs :href uri-to-url)
        cover-url (some->> (hs/select (hs/tag :img) node) first :attrs :src uri-to-url)
        id (some->> (re-seq #"itemid=(\d+)&" book-url) first second)]
    (when title
      {:title title
       :book-url book-url
       :cover-url cover-url
       :id id})))

(defn- prepare-directory [{:keys [id title book-url cover-url] :as book}]
  (when book
    (let [meta-file (io/file "books" (str id "_" title) "meta.txt")]
      (when-not (.exists meta-file)
        (io/make-parents meta-file)
        (spit meta-file (s/join "\n" [id title book-url cover-url]))))))

;; 按页获取书籍信息
(defn get-all-books! [url]
  (t/info "get books page" url)
  (let [{:keys [body status error]} @(web-get url)]
    (when (not= 200 status)
      (println error))
    (when-let [root (some->> body parse as-hickory)]
      (let [x-book (some->> root
                            (hs/select (hs/class :item))
                            (map parse-item))
            next-hidden? (some->> root
                                  (hs/select (hs/and (hs/class "next")
                                                     (hs/class "hidden")))
                                  seq)
            next-page (when-not next-hidden?
                        (some->> root
                                 (hs/select (hs/class "next"))
                                 first :content first :attrs :href))]
        (run! prepare-directory x-book)
        (when next-page
          (recur (uri-to-url next-page)))))))

(defn- parse-image-link [node]
  {:uri (some->> (hs/select (hs/tag :a) node) first :attrs :href)
   :title (some->> (hs/select (hs/tag :img) node) first :attrs :title)})

;; 下载书籍图片
(defn download-book! [meta-file]
  (with-open [rdr (io/reader meta-file)]
    (let [[id title book-url cover-url] (line-seq rdr)
          parent (.getParent meta-file)
          cover-file (io/file parent "cover.jpg")]
      (when-not (.exists cover-file)
        (with-open [in (io/input-stream cover-url)
                    out (io/output-stream cover-file)]
          (io/copy in out)))
      (let [{:keys [body status error]} @(web-get book-url)]
        (when (not= 200 status)
          (println error))
        (when-let [root (some->> body parse as-hickory)]
          (let [img-links (some->> root
                                   (hs/select (hs/class "content"))
                                   (map parse-image-link))]
            (doseq [{:keys [title uri]} img-links]
              (let [img-file (io/file parent (str title ".jpg"))]
                (when-not (.exists img-file)
                  (with-open [in (io/input-stream (uri-to-url uri))
                              out (io/output-stream img-file)]
                    (io/copy in out)))))))))))

(defn download-all-books! [n-thread]
  (let [x-meta-file (->> (file-seq (io/file "books/"))
                         (filter (fn [f]
                                   (= (.getName f) "meta.txt")))
                         reverse)]
    (cp/pdoseq
     n-thread
     [file x-meta-file]
     (t/info "Download" (.getName (.getParentFile file)))
     (download-book! file))))

(defn -main [& [cmd & args]]
  (case cmd
    "pre-fetch" (get-all-books! (first args))
    "download" (download-all-books! (or (str->num (first args)) 2))
    (println "用法:

下载书籍列表，将<url>换为书籍列表页：
java -jar clj-hua-0.1.0-SNAPSHOT-standalone.jar pre-fetch <url>

下载书籍内容，须上在一命令完成后执行，将<n-thread>替换为线程数，建议不要超过4：
java -jar clj-hua-0.1.0-SNAPSHOT-standalone.jar download <n-thread>
"))
  (shutdown-agents))

;; usage
;; - Get all book information by pages
;; (get-all-books! url)
;; - Download all images by book
;; 
