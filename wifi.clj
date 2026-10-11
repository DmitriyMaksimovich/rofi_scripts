#!/usr/bin/env bb

(ns wifi
  (:require [clojure.string :as s]
            [clojure.java.shell :refer [sh]]
            [babashka.process :as p]
            [utils :refer [run-nmcli-list-command rofi-select]]))

(def max-connections-to-display 10)
(def theme-file "./themes/network.rasi")
(def show-wifi-cli ["rofi" "-dmenu" "-p" "Select Wi-Fi:"
                    "-theme" theme-file "-theme-str" "element-text { horizontal-align: 0; }"])

(defn get-all-active-connections []
  (run-nmcli-list-command (sh "nmcli" "-t" "-f" "NAME,TYPE,DEVICE" "connection" "show")))

(defn list-wifi []
  (run-nmcli-list-command (sh "nmcli" "-t" "-f" "IN-USE,SSID,BAND,BARS" "device" "wifi" "list")))

(defn get-wifi-connections []
  (map (fn [[in-use ssid band bars]]
         {:active (or (= "*" in-use) (= "yes" in-use))
          :name   ssid
          :band   band
          :bars   bars})
       (list-wifi)))

(defn get-active-wifi [connections]
  (some #(when (:active %) %) connections))

(defn get-active-wifi-index [connections]
  (first (keep-indexed #(when (:active %2) %1) connections)))

(defn rofi-password []
  (let [result (p/shell ["rofi" "-dmenu" "-password" "-p" "Password: "
                         "-theme" theme-file]
                        {:out :string :continue true})
        pw (some-> (:out result) s/trim)]
    (when (and (zero? (:exit result)) (not (s/blank? pw)))
      pw)))

;; TODO handle error when password is wrong - display some message
(defn connect-wifi [ssid]
  (let [result (sh "nmcli" "device" "wifi" "connect" ssid)]
    (when (and (not (zero? (:exit result)))
               (s/includes? (:out result) "password is required"))
      (let [password (rofi-password)]
        (when password
          (sh "nmcli" "device" "wifi" "connect" ssid "password" password))))))

(defn disconnect-wifi [ssid]
  (zero? (:exit (sh "nmcli" "connection" "down" ssid))))

(defn toggle-wifi [ssid]
  (let [active-ssid (:name (get-active-wifi (get-wifi-connections)))]
    (if (and active-ssid (= ssid active-ssid))
      (disconnect-wifi ssid)
      (if active-ssid
        (do (disconnect-wifi ssid) (connect-wifi ssid))
        (connect-wifi ssid)))))

(defn format-wifi-entry
  "Format a Wi-Fi network entry into an aligned string.
   Takes a map with :name, :bars, and :band keys.
   Name is padded/truncated to 15 characters."
  [{:keys [name bars band]}]
  (let [;; Truncate to 20 chars, then pad with spaces to exactly 20
        ;; TODO if name is cutted, then selection will recurt partial SSID, with cannot be used to connet
        formatted-name (->> (subs name 0 (min 20 (count name)))
                            (format "%-20s"))
        formatted-bars (format "%-6s" bars)
        formatted-band (format "%-4s" (str band))]
    (str formatted-name "| " formatted-bars "| " formatted-band)))

(defn clear-selected [selection]
  (s/trim (first (s/split selection #"\|"))))

(defn main-rofi []
  (let [wifi-connections (get-wifi-connections)
        named-connections (filter #(not-empty (:name %)) wifi-connections) ;; drop connections without name
        formatted-connections (map format-wifi-entry named-connections)
        connections (s/join "\n" formatted-connections)
        lines (max (count formatted-connections) max-connections-to-display)
        active-index (get-active-wifi-index wifi-connections)
        cli-lines (conj show-wifi-cli "-theme-str" (str "listview { lines: " lines "; }"))
        cli (if active-index
              (concat cli-lines ["-a" active-index])
              cli-lines)
        {:keys [exit selection]} (rofi-select cli connections)]
    (case exit
      0 (do
          (toggle-wifi (clear-selected (apply str selection)))
          (recur)
          )
      1 (println "Cancelled")
      (println "Rofi exited with code:" exit))))
