#!/usr/bin/env bb

(ns network-main-menu
  (:require [clojure.string :as s]
            [utils :refer [rofi-select]]
            [vpn :as vpn]
            [wifi :as wifi]))

(def theme-file "./themes/network.rasi")

(def vpn-str "VPN")
(def wifi-str "Wi-Fi")

(def show-main-menu-cli ["rofi" "-dmenu" "-p" "Select action:"
                         "-theme" theme-file "-theme-str" "listview { lines: 2; }"])
(def main-menu-options (s/join "\n" [vpn-str wifi-str]))

(defn -main []
  (let [{:keys [exit selection]} (rofi-select show-main-menu-cli main-menu-options)]
    (case exit
      0 (condp = selection
          vpn-str (vpn/main-rofi)
          wifi-str (wifi/main-rofi))
      1 nil ;; exit on ESC
      (println "Rofi exited with code:" exit))))

(apply -main *command-line-args*)
