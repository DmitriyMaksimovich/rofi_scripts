#!/usr/bin/env bb

(ns network
  (:require [babashka.process :as p]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]))

(def vpn-theme-file "~/Dev/rofi_scripts/themes/network.rasi")

;;;; WIFI

(defn- run-nmcli-list-command
  "Run f and parse typical nmcli list formatted response"
  [f]
  (map #(s/split % #":")
       (s/split (:out f) #"\n")))

(defn get-all-active-connections []
  (run-nmcli-list-command (sh "nmcli" "-t" "-f" "NAME,TYPE,DEVICE" "connection" "show")))

(defn list-wifi []
  (run-nmcli-list-command (sh "nmcli" "-t" "-f" "IN-USE,SSID,BAND,BARS" "device" "wifi" "list")))

(defn get-active-wifi [connections]
  (some #(when (= "wlan0" (last %)) %) connections))

;;;; VPN

(defn get-wireguard-connections []
  (let [connections (filter #(= "wireguard" (second %))
                            (run-nmcli-list-command (sh "nmcli" "-t" "-f" "NAME,TYPE,DEVICE" "connection" "show")))]
    (map #(if (= 3 (count %))
            (list "*" (first %))
            (list " " (first %)))
         connections)))

(defn get-active-vpn-name []
  (some #(when (= "*" (first %)) (second %)) (get-wireguard-connections)))

(defn get-active-vpn-indexes []
  (let [connections (get-wireguard-connections)]
    (keep-indexed #(when (= "*" (first %2)) %1) connections)))

(defn activate-vpn [region]
  (zero? (:exit (sh "nmcli" "connection" "up" region))))

(defn deactivate-vpn [region]
  (zero? (:exit (sh "nmcli" "connection" "down" region))))

(defn toggle-vpn [region]
  (if (= region (get-active-vpn-name))
    (deactivate-vpn region)
    (activate-vpn region)))

;;;; ROFI

(defn rofi-select []
  (let [connections (get-wireguard-connections)
        active-connection-indexes (get-active-vpn-indexes)
        cli ["rofi" "-dmenu" "-p" "Select VPN:" "-theme" vpn-theme-file]
        result (p/shell (if-not (empty?   active-connection-indexes)
                          (concat cli ["-a" (s/join "," active-connection-indexes)])
                          cli)
                        {:in (s/join "\n" (map second connections))
                         :out :string
                         :continue true})]
    {:exit (:exit result)
     :selection (when (zero? (:exit result))
                  (s/trim (:out result)))}))

;;;; MAIN

(defn -main []
  (loop []
    (let [{:keys [exit selection]} (rofi-select)]
      (case exit
        0 (do
            (toggle-vpn selection)
            (recur))
        1 (println "Cancelled")
        (println "Rofi exited with code:" exit)))))

(apply -main *command-line-args*)
