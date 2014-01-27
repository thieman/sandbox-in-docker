(ns sandbox.master
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :refer [info warn]]
            [clojure.set :refer [difference]]
            [clojure.core.async :refer [<! >! go chan sliding-buffer]]
            [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [sandbox.docker :as docker]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn process-message [message]
  (info message)
  (when (and (= "message" (:type message))
             (not= (get-in message [:message :sender_email])
                   (get-in conn [:opts :username])))
    (let [content (get-in message [:message :content])
          forms (extract-forms content)
          allowed-forms (filter (partial allowed-form? content) forms)]
      (when (seq allowed-forms)
        (go (let [result-channel (docker/eval-on-worker (first allowed-forms))
                  result (<! result-channel)
                  reply (str (first allowed-forms) "\n" "=> " result)]
              (respond message reply)))))))

(defroutes app-routes
  (POST "/clojure" {params :params} (process-message)))

(def app (-> app-routes
             (wrap-json-response)
             (wrap-json-params)))

(defn -main []
  (docker/build-container!)
  (docker/add-worker!)
  (run-jetty (app) {:port 8080 :join? false}))
