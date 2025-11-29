(ns clojure-lab3.core
  (:gen-class)
  (:require [clojure.test :refer [deftest is testing run-tests]]))

(defn parallel-filter
  "Параллельная версия filter, обрабатывающая блоки элементов"
  ([pred coll]
   (parallel-filter pred 1000 coll))

  ([pred chunk-size coll]
   (let [chunks (partition-all chunk-size coll)
         futures (map (fn [chunk]
                        (future
                          (filter pred chunk)))
                      chunks)]
     (->> futures
          (map deref)
          (apply concat)))))
;; ->> начинаем с


(deftest test-parallel-filter-basic
  (testing "Фильтрация четных чисел"
    (let [input [1 2 3 4 5 6 7 8 9 10]
          expected (filter even? input)
          result (parallel-filter even? 3 input)]
      (is (= expected result))
      (is (= [2 4 6 8 10] result))))

  (testing "Пустая последовательность"
    (is (empty? (parallel-filter even? [])))
    (is (empty? (parallel-filter even? 5 []))))

  (testing "Все элементы удовлетворяют предикату"
    (let [input [2 4 6 8 10]
          result (parallel-filter even? 2 input)]
      (is (= input result))))

  (testing "Ни один элемент не удовлетворяет предикату"
    (let [input [1 3 5 7 9]
          result (parallel-filter even? 2 input)]
      (is (empty? result)))))

(deftest test-parallel-filter-infinite
  (testing "Бесконечная последовательность"
    (let [result (->> (range)
                      (parallel-filter even? 100)
                      (take 10))]
      (is (= [0 2 4 6 8 10 12 14 16 18] result))))

  (testing "Бесконечная последовательность с предикатом"
    (let [result (->> (iterate inc 1)
                      (parallel-filter #(> % 5) 50)
                      (take 5))]
      (is (= [6 7 8 9 10] result)))))

(deftest test-parallel-filter-edge-cases
  (testing "Разные размеры блоков"
    (let [input [1 2 3 4 5 6 7 8 9 10]
          result-small (parallel-filter odd? 2 input)
          result-medium (parallel-filter odd? 5 input)
          result-large (parallel-filter odd? 20 input)]
      (is (= [1 3 5 7 9] result-small result-medium result-large))))

  (testing "Один элемент"
    (is (= [2] (parallel-filter even? 5 [2])))
    (is (empty? (parallel-filter even? 5 [1]))))

  (testing "Размер блока = 1"
    (let [input [1 2 3 4 5]
          result (parallel-filter even? 1 input)]
      (is (= [2 4] result)))))

(defn heavy-predicate
  "Тяжелый предикат для демонстрации производительности"
  [x]
  (Thread/sleep 1)
  (even? x))

(defn demonstrate-efficiency
  "Демонстрация производительности parallel filter"
  []
  (let [data (range 50)
        chunk-sizes [1 5 10 25]]

    (println "Последовательный filter:")
    (time (doall (filter heavy-predicate data)))

    (println "Параллельный filter:")
    (doseq [chunk-size chunk-sizes]
      (println "Блоки по" chunk-size)
      (time (doall (parallel-filter heavy-predicate chunk-size data))))))

(defn -main
  "Главная функция с демонстрацией работы parallel-filter"
  [& args]
  (println "=== Parallel Filter ===")

  (println "Запуск тестов...")
  (let [test-results (run-tests 'clojure-lab3.core)]
    (if (zero? (+ (:fail test-results 0) (:error test-results 0)))
      (println "Все тесты пройдены")
      (println "Есть непройденные тесты")))

  (println "Демонстрация работы:")
  (let [numbers [1 2 3 4 5 6 7 8 9 10 11 12]
        result (parallel-filter even? 3 numbers)]
    (println "Исходные данные:" numbers)
    (println "Результат:" result))

  (let [result (->> (range)
                    (parallel-filter even? 5)
                    (take 5))]
    (println "Первые 5 четных чисел:" result))

  (shutdown-agents))

(comment
  (parallel-filter even? [1 2 3 4 5 6 7 8 9 10])
  (parallel-filter #(> % 5) 3 [1 2 3 4 5 6 7 8 9 10])
  (->> (range)
       (parallel-filter #(zero? (mod % 3)) 100)
       (take 10))
  (parallel-filter #(.startsWith % "a") 2 ["apple" "banana" "apricot" "cherry"])
  (time (doall (parallel-filter heavy-predicate 1 (range 20))))
  (parallel-filter #(and (even? %) (> % 10)) 4 [5 8 12 15 18 20 25]))