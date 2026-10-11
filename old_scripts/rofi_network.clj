#!/usr/bin/env bb

;; -mode require providng a list on start, but getting list of wifi may take a time, making
;; start slow; also item\list css would be shared between all mods, so it is hard to dipslay
;; lists of diffrent sized, lie 3 vpn and 10+ wifi option

(ns rofi-network
  (:require [babashka.process :as p]))

(def vpn-script
  (str (System/getenv "HOME") "/Dev/rofi_scripts/vpn.clj"))

(def wifi-script
  (str (System/getenv "HOME") "/Dev/rofi_scripts/wifi.clj"))

(def network-theme-file
  (str (System/getenv "HOME") "/Dev/rofi_scripts/themes/network.rasi"))

(defn -main []
  (let [cmd ["rofi"
             "-modi" (str "vpn:" vpn-script ",wifi:" wifi-script)
             "-show" "vpn"
             "-theme" network-theme-file
             "-sidebar-mode"]
        result (p/shell cmd {:continue true})]
    (System/exit (:exit result))))

(apply -main *command-line-args*)
