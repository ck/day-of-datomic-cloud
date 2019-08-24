;   Copyright (c) Cognitect, Inc. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(require '[datomic.client.api :as d]
         '[datomic.samples.repl :as repl])
(import '(java.util UUID))

(def client-config (read-string (slurp "config.edn")))
(def client (d/client client-cfg))
(def db-name (str "scratch-" (UUID/randomUUID)))
(d/create-database client {:db-name db-name})
(def conn (d/connect client {:db-name db-name}))

(repl/transact-all conn (repl/resource "day-of-datomic-cloud/social-news.edn"))

;; create a story and some comments
(let [[story comment-1 comment-2] (map str (repeat "tempid-") (range))
      {:keys [tempids db-after]}
      (d/transact conn {:tx-data [{:db/id story
                                   :story/title "Getting Started"
                                   :story/url "http://docs.datomic.com/getting-started.html"}
                                  {:db/id comment-1
                                   :comment/body "It woud be great to learn about component attributes."
                                   :_comments story}
                                  {:db/id comment-2
                                   :comment/body "I agree."
                                   :_comments comment-1}]})]
  (def story (get tempids story))
  (def comment-1 (get tempids comment-1))
  (def comment-2 (get tempids comment-2))
  (def db db-after))

;; component attributes are automatically pulled when you pull the parent
(d/pull db '[*] story)

;; what does db.fn/retractEntity do?
(:db/doc (d/pull db '[:db/doc] :db/retractEntity))

;; retract the story
(def retracted-es (->> (d/transact conn {:tx-data [[:db/retractEntity story]]})
                       :tx-data
                       (remove :added)
                       (map :e)
                       (into #{})))

;; retraction recursively retracts component comments
(assert (= retracted-es #{story comment-1 comment-2}))

(d/delete-database client {:db-name db-name})
