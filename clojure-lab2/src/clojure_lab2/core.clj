(ns clojure-lab2.core
  (:gen-class)
  (:require [clojure.test :refer [deftest is run-tests testing]]))


(defn primes
  "Возвращает ленивую бесконечную последовательность простых чисел"
  []
  (letfn [
          (sieve [stream]
            (lazy-seq 

             (let [prime (first stream)
                   
                   rest-stream (rest stream)
                   
                   filtered (remove #(zero? (mod % prime)) rest-stream)] 
               
               (cons prime (sieve filtered)))))]
    
    (sieve (iterate inc 2))))

(defn first-n-primes
  "Возвращает первые n простых чисел"
  [n] 
  (take n (primes)))

(defn primes-less-than
  "Возвращает все простые числа меньше заданного предела"
  [limit] 
  (take-while #(< % limit) (primes)))

(defn nth-prime
  "Возвращает n-ое простое число (нумерация с 1)"
  [n] 
  (nth (primes) (dec n)))


(deftest test-first-n-primes
  (testing "Первые 10 простых чисел"
    (is (= (first-n-primes 10) [2 3 5 7 11 13 17 19 23 29])))

  (testing "Первые 5 простых чисел"
    (is (= (first-n-primes 5) [2 3 5 7 11])))

  (testing "Первое простое число"
    (is (= (first-n-primes 1) [2]))))

(deftest test-primes-less-than
  (testing "Простые числа меньше 20"
    (is (= (primes-less-than 20) [2 3 5 7 11 13 17 19])))

  (testing "Простые числа меньше 10"
    (is (= (primes-less-than 10) [2 3 5 7])))

  (testing "Простые числа меньше 3"
    (is (= (primes-less-than 3) [2]))))

(deftest test-nth-prime
  (testing "Конкретные простые числа по порядку"
    (is (= (nth-prime 1) 2))
    (is (= (nth-prime 2) 3))
    (is (= (nth-prime 5) 11))
    (is (= (nth-prime 10) 29))))

(deftest test-primes-properties
  (testing "Свойства простых чисел"
    (let [first-15 (first-n-primes 15)]
      (is (every? #(> % 1) first-15))
      (is (= (count first-15) (count (distinct first-15)))))))


(defn -main
  "Демонстрация работы последовательности простых чисел"
  [& args]
  (println "=== Бесконечная последовательность простых чисел ===")

  (println "\n1. Первые 20 простых чисел:")
  (println "   Результат:" (first-n-primes 20))

  (println "\n2. Простые числа меньше 50:")
  (println "   Результат:" (primes-less-than 50))

  (println "\n3. 100-ое простое число:")
  (println "   Результат:" (nth-prime 100))

  (println "\n4. Запуск тестов...")
  (run-tests 'clojure-lab2.core)

  (println "\n⭐ Готово! Последовательность работает и готова к использованию."))