#!/usr/bin/env bb

(ns utils
  (:require [clojure.string :as s]
            [babashka.process :as p]))

(defn run-nmcli-list-command
  "Run f and parse typical nmcli list formatted response"
  [f]
  (map #(s/split % #":")
       (s/split (:out f) #"\n")))

(defn rofi-select [cli-command options]
  (let [result (p/shell cli-command
                        {:in options
                         :out :string
                         :continue true})]
     {:exit (:exit result)
      :selection (when (zero? (:exit result))
                   (s/trim (:out result)))}))
