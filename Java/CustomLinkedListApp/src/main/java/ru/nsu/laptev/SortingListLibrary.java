import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

// Главный класс с библиотечной реализацией списка
public class SortingListLibrary {
    private static List<String> list = Collections.synchronizedList(new ArrayList<>());
    private static final int THREAD_COUNT = 3;
    private static List<Thread> sortingThreads = new ArrayList<>();
    private static final AtomicInteger totalSteps = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=== Программа с библиотечным списком ===");
        System.out.println("Вводите строки (пустая строка для вывода, 'exit' для выхода):");

        // Запуск потоков сортировки
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread sortingThread = new Thread(new LibrarySortingTask(list, totalSteps), "LibrarySortThread-" + i);
            sortingThread.setDaemon(true);
            sortingThread.start();
            sortingThreads.add(sortingThread);
        }

        // Основной цикл ввода
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                break;
            } else if (input.isEmpty()) {
                // Вывод текущего состояния списка
                synchronized(list) {
                    System.out.println("=== Текущее состояние списка (" + list.size() + " элементов) ===");
                    for (String item : list) {
                        System.out.println(item);
                    }
                }
                System.out.println("Всего шагов сортировки: " + totalSteps.get());
                System.out.println("================================");
            } else {
                // Разбивка и добавление строк
                List<String> parts = splitString(input);
                synchronized(list) {
                    for (int i = parts.size() - 1; i >= 0; i--) {
                        list.add(0, parts.get(i));
                    }
                }
                System.out.println("Добавлено: " + input);
            }
        }

        scanner.close();
        System.out.println("Программа завершена. Всего шагов: " + totalSteps.get());
    }

    // Разбивка строки на части по 80 символов
    private static List<String> splitString(String s) {
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < s.length()) {
            int end = Math.min(index + 80, s.length());
            parts.add(s.substring(index, end));
            index = end;
        }
        return parts;
    }
}

// Задача сортировки для библиотечного списка
class LibrarySortingTask implements Runnable {
    private final List<String> list;
    private final AtomicInteger totalSteps;
    private int localSteps = 0;

    public LibrarySortingTask(List<String> list, AtomicInteger totalSteps) {
        this.list = list;
        this.totalSteps = totalSteps;
    }

    @Override
    public void run() {
        while (true) {
            try {
                boolean swapped = bubbleSortStep();
                localSteps++;
                totalSteps.incrementAndGet();

                // Задержка между шагами
                Thread.sleep(1000);

                if (localSteps % 10 == 0) {
                    System.out.println(Thread.currentThread().getName() + " выполнил " + localSteps + " шагов");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Шаг пузырьковой сортировки для ArrayList
    private boolean bubbleSortStep() throws InterruptedException {
        synchronized(list) {
            boolean swapped = false;

            for (int i = 0; i < list.size() - 1; i++) {
                // Имитация задержки внутри шага
                Thread.sleep(10);

                if (list.get(i).compareTo(list.get(i + 1)) > 0) {
                    // Обмен элементами (в ArrayList меняем содержимое, а не ссылки)
                    Collections.swap(list, i, i + 1);
                    swapped = true;
                }
            }
            return swapped;
        }
    }
}
