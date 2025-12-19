(ns clojure-lab4.core
  (:gen-class))

(declare supply-msg)
(declare notify-msg)

;; ================= STORAGE =================

(defn storage
  [ware notify-step & consumers]
  (let [counter (atom 0 :validator #(>= % 0))
        worker-state {:storage counter
                      :ware ware
                      :notify-step notify-step
                      :consumers consumers}]
    {:storage counter
     :ware ware
     :worker (agent worker-state)}))

;; ================= FACTORY =================

(defn factory
  [amount duration target-storage & ware-amounts]
  (let [bill (apply hash-map ware-amounts)
        buffer (reduce-kv (fn [acc k _] (assoc acc k 0)) {} bill)
        worker-state {:amount amount
                      :duration duration
                      :target-storage target-storage
                      :bill bill
                      :buffer buffer}]
    {:worker (agent worker-state)}))

;; ================= SOURCE =================

(defn source
  [amount duration target-storage]
  (new Thread
       (fn loop []
         (Thread/sleep duration)
         (send (target-storage :worker) supply-msg amount)
         (recur))))

;; ================= STORAGE MESSAGE =================

(defn supply-msg
  [state amount]
  (swap! (state :storage) + amount)
  (let [ware (state :ware)
        cnt @(state :storage)
        notify-step (state :notify-step)
        consumers (state :consumers)]
    ;; logging
    (when (and (> notify-step 0)
               (> (int (/ cnt notify-step))
                  (int (/ (- cnt amount) notify-step))))
      (println (.format (java.text.SimpleDateFormat. "HH:mm:ss.SSS")
                        (java.util.Date.))
               "|" ware "amount:" cnt))
    ;; notify factories (FIX: only if consumers exist)
    (when (seq consumers)
      (doseq [consumer (shuffle consumers)]
        (send (consumer :worker)
              notify-msg ware (state :storage) amount))))
  state)

;; ================= FACTORY MESSAGE =================

(defn notify-msg
  [state ware storage-atom amount]
  (let [{:keys [bill buffer amount duration target-storage]} state
        need (get bill ware)]
    ;; ресурс фабрике не нужен
    (if-not need
      state
      (let [missing (- need (get buffer ware 0))
            take (min amount (max 0 missing))]
        (if (pos? take)
          (try
            ;; атомарно забираем ресурс
            (swap! storage-atom - take)
            (let [state (update-in state [:buffer ware] + take)
                  buffer (:buffer state)]
              ;; проверяем, можно ли запускать цикл
              (if (every?
                   (fn [[w n]] (>= (get buffer w 0) n))
                   bill)
                (do
                  ;; производственный цикл
                  (Thread/sleep duration)
                  ;; списываем ресурсы
                  (let [new-buffer
                        (reduce-kv
                         (fn [b w n] (update b w - n))
                         buffer
                         bill)]
                    ;; отправляем продукт
                    (send (target-storage :worker) supply-msg amount)
                    (assoc state :buffer new-buffer)))
                state))
            (catch IllegalStateException _
              ;; ресурсов не хватило — спокойно выходим
              state))
          state)))))

;; ================= CONFIGURATION =================

(def safe-storage
  (storage "Safe" 1))

(def safe-factory
  (factory 1 3000 safe-storage "Metal" 3))

(def cuckoo-clock-storage
  (storage "Cuckoo-clock" 1))

(def cuckoo-clock-factory
  (factory 1 2000 cuckoo-clock-storage "Lumber" 5 "Gears" 10))

(def gears-storage
  (storage "Gears" 20 cuckoo-clock-factory))

(def gears-factory
  (factory 4 1000 gears-storage "Ore" 4))

(def metal-storage
  (storage "Metal" 5 safe-factory))

(def metal-factory
  (factory 1 1000 metal-storage "Ore" 10))

(def lumber-storage
  (storage "Lumber" 20 cuckoo-clock-factory))

(def lumber-mill
  (source 5 4000 lumber-storage))

(def ore-storage
  (storage "Ore" 10 metal-factory gears-factory))

(def ore-mine
  (source 2 1000 ore-storage))

;; ================= CONTROL =================

(defn start []
  (.start ore-mine)
  (.start lumber-mill)
  (println "Production started"))

(defn stop []
  (.stop ore-mine)
  (.stop lumber-mill)
  (println "Production stopped"))

;; ================= MAIN =================

(defn -main [& _]
  (println "=== Factory simulation started ===")
  (start)
  ;; держим JVM живой
  (loop []
    (Thread/sleep 100000)
    (recur)))
