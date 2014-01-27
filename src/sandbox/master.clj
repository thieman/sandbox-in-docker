(ns sandbox.master
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :refer [info warn]]
            [clojure.set :refer [difference]]
            [clojure.core.async :refer [<! >! go chan sliding-buffer]]
            [com.keminglabs.zmq-async.core :refer [register-socket!]]
            [sandbox.docker :as docker]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer :all]
            [org.httpkit.server :refer :all]))

(defn message-handler [req]
  (info req)
  (with-channel req req-channel
    (go (let [code (get-in req [:body :code])]
          (if code
            (let [result-channel (docker/eval-on-worker code)
                  result (<! result-channel)
                  reply (str code "\n" "=> " result)]
              (send! req-channel {:status 200
                                  :headers {"Content-Type" "application/json"}
                                  :body reply}))
            (send! req-channel {:status 400}))))))

(defroutes app-routes
  (POST "/clojure" [] message-handler))

(def app (-> app-routes
             (wrap-json-response)
             (wrap-json-body)))

(defn -main []
  (docker/build-container!)
  (docker/add-worker!)
  (run-server app {:port 8080}))
