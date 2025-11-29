(ns clojure-lab1.core
  (:gen-class))

(defn generate-strings [alphabet n]
  "Генерирует все строки длины N из заданного алфавита, 
   где НЕТ двух одинаковых символов подряд"

  
  (let [initial-strings (map str alphabet)] 
    (reduce (fn [current-strings _]
              
              (mapcat (fn [current-string]
                        
                        (let [last-char (last current-string)
                              
                              allowed-chars (remove #(= (str %) (str last-char)) alphabet)]
                          
                          (map (fn [char-to-add]
                                 
                                 (str current-string char-to-add))
                               allowed-chars)))
                      current-strings))
            
            initial-strings
            
            (range 1 n))))

(defn -main
  "Я тут могу писать чо хочу, прикол?"
  [& args]
  
  (println "=== Задача C1 ===")
  (println "Алфавит 1: [\"a\" \"b\" \"c\"], N=2")
  (let [result (generate-strings ["a" "b" "c"] 2)]
    (println "Гига результат:" result) )

  (println " \nАлфавит 2: [\"x\" \"y\"], N=3")
  (println "Гига результат: (\"xyx\" \"yxy\")")
  (let [result2 (generate-strings ["x" "y"] 3)] )

  (println "\nАлфавит 3: [\"1\" \"2\"], N=1")
  (println "Гига результат: (\"1\" \"2\")")
  (let [result3 (generate-strings ["1" "2"] 1)]
  ))