(ns gaia.core
  (:require [clojure.core.async :as a]
            [discljord.messaging :as m]
            [discljord.connections :as c]
            [dotenv]))

(def token      (dotenv/env "DISCORD_TOKEN"))
(def intents    #{:guilds :guild-messages})
(def channel-id "1018526450254626841")

(def command-to-response
  {"!ping" "pong"
   "!plant" "1 tree has been planted"
   "!cut" "1 tree has been cut"
   "!leaderboard" "1 leoo#1234 (150)\n2 enzoledebilo#7896 (56)"})

(defn send-msg [message-ch channel-id content]
  (m/create-message! message-ch channel-id :content content))

(defn respond-to [message-ch channel-id message]
  (if (command-to-response message)
    (send-msg message-ch channel-id (command-to-response message))))


(defn -main []
  (let [event-ch      (a/chan 100)
        connection-ch (c/connect-bot! token event-ch :intents intents)
        message-ch    (m/start-connection! token)]
  (try
    (loop []
      (let [[event-type event-data] (a/<!! event-ch)]
        ; when message
        (when (and (= event-type :message-create)
                   (= (:channel-id event-data) channel-id )
                   (not (:bot (:author event-data))))
          (println (keys event-data))
          (println (str ":content " (event-data :content)))
          ;; send message here
          (let [message (event-data :content)]
            (respond-to message-ch channel-id message)))

        ; stop on pin
        (when (= :channel-pins-update event-type)
          (c/disconnect-bot! connection-ch))

        ; recur
        (when-not (= :disconnect event-type)
          (recur))))
    (finally
      (m/stop-connection! message-ch)
      (a/close!           event-ch)))))



(-main)