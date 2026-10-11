#!/usr/bin/env bb

(ns vpn
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [utils :refer [run-nmcli-list-command rofi-select]]))

(defn get-wireguard-connections
  ;; Return list of connections, where elem 0 = "*" for active connection
  []
  (let [connections (filter #(= "wireguard" (second %))
                            (run-nmcli-list-command (sh "nmcli" "-t" "-f" "NAME,TYPE,DEVICE" "connection" "show")))]
    (map #(if (= 3 (count %)) ;; ["*" "name" "type"] -> active; ["name" "type"] -> not active
            (list "*" (first %))
            (list " " (first %)))
         connections)))

(defn get-active-vpn-name
  ([]
   (get-active-vpn-name (get-wireguard-connections)))
  ([wireguard-connections]
   (some #(when (= "*" (first %)) (second %)) wireguard-connections)))

(defn get-active-vpn-indexes
  ([]
   (get-active-vpn-indexes (get-wireguard-connections)))
  ([wireguard-connections]
     (keep-indexed #(when (= "*" (first %2)) %1) wireguard-connections)))

(defn activate-vpn [region]
  (zero? (:exit (sh "nmcli" "connection" "up" region))))

(defn deactivate-vpn [region]
  (zero? (:exit (sh "nmcli" "connection" "down" region))))

(defn toggle-vpn [region]
  (if (= region (get-active-vpn-name))
    (deactivate-vpn region)
    (activate-vpn region)))

(def theme-file "./themes/network.rasi")
(def show-vpn-cli ["rofi" "-dmenu" "-p" "Select VPN:" "-theme" theme-file])

(defn main-rofi []
  (let [connections (get-wireguard-connections)
        active-connection-indexes (get-active-vpn-indexes)
        connection-names (s/join "\n" (map second connections))
        cli (if-not (empty? active-connection-indexes)
              (concat show-vpn-cli ["-a" (s/join "," active-connection-indexes)])
              show-vpn-cli)
        {:keys [exit selection]} (rofi-select cli connection-names)]
    (case exit
      0 (do
          (toggle-vpn selection)
          (recur))
      1 (println "Cancelled")
      (println "Rofi exited with code:" exit))))
