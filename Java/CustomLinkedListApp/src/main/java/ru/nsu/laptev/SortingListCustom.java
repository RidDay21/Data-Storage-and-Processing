import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Узел двусвязного списка
class Node {
    String data;
    Node next;
    Node prev;

    public Node(String data) {
        this.data = data;
    }
}

// Кастомный двусвязный список с синхронизацией
class CustomLinkedList implements Iterable<String> {
    private Node head;
    private Node tail;
    private final Object lock = new Object();
    private final AtomicInteger size = new AtomicInteger(0);

    public void addFirst(String data) {
        if (data == null) return;

        List<String> parts = splitString(data);

        synchronized(lock) {
            for (int i = parts.size() - 1; i >= 0; i--) {
                String part = parts.get(i);
                Node newNode = new Node(part);

                if (head == null) {
                    head = tail = newNode;
                } else {
                    newNode.next = head;
                    head.prev = newNode;
                    head = newNode;
                }
                size.incrementAndGet();
            }
        }
    }

    // Разбивка строки на части по 80 символов
    private List<String> splitString(String s) {
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < s.length()) {
            int end = Math.min(index + 80, s.length());
            parts.add(s.substring(index, end));
            index = end;
        }
        return parts;
    }

    public boolean bubbleSortStep() throws InterruptedException {
        synchronized(lock) {
            if (head == null || head.next == null) {
                return false;
            }

            boolean swapped = false;
            Node current = head;

            while (current != null && current.next != null) {
                // Захватываем узлы в порядке от головы к хвосту (для предотвращения deadlock)
                Node first = current;
                Node second = current.next;

                // Имитация задержки
                Thread.sleep(10);

                if (first.data.compareTo(second.data) > 0) {
                    swapNodes(first, second);
                    swapped = true;

                    // Если поменяли head, обновляем указатель
                    if (first == head) {
                        head = second;
                    }
                    if (second == tail) {
                        tail = first;
                    }
                }
                current = current.next;
            }
            return swapped;
        }
    }

    private void swapNodes(Node a, Node b) {
        if (a.next != b) {
            throw new IllegalArgumentException("Nodes must be adjacent");
        }

        Node aPrev = a.prev;
        Node bNext = b.next;

        if (aPrev != null) {
            aPrev.next = b;
        }
        if (bNext != null) {
            bNext.prev = a;
        }

        // Обновляем ссылки между a и b
        a.next = bNext;
        a.prev = b;
        b.next = a;
        b.prev = aPrev;
    }

    //для вывода
    public List<String> toList() {
        synchronized(lock) {
            List<String> result = new ArrayList<>();
            Node current = head;
            while (current != null) {
                result.add(current.data);
                current = current.next;
            }
            return result;
        }
    }

    @Override
    public Iterator<String> iterator() {
        return toList().iterator();
    }

    public int size() {
        return size.get();
    }
}

// Главный класс с собственной реализацией списка
public class SortingListCustom {
    private static CustomLinkedList list = new CustomLinkedList();
    private static final int THREAD_COUNT = 3;
    private static List<Thread> sortingThreads = new ArrayList<>();
    private static final AtomicInteger totalSteps = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("=== Программа с собственным списком ===");
        System.out.println("Вводите строки (пустая строка для вывода, 'exit' для выхода):");

        // Запуск потоков сортировки
        for (int i = 0; i < THREAD_COUNT; i++) {
            Thread sortingThread = new Thread(new SortingTask(list, totalSteps), "SortingThread-" + i);
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
                List<String> currentList = list.toList();
                System.out.println("=== Текущее состояние списка (" + currentList.size() + " элементов) ===");
                for (String item : currentList) {
                    System.out.println(item);
                }
                System.out.println("Всего шагов сортировки: " + totalSteps.get());
                System.out.println("================================");
            } else {
                list.addFirst(input);
                System.out.println("Добавлено: " + input);
            }
        }

        scanner.close();
        System.out.println("Программа завершена. Всего шагов: " + totalSteps.get());
    }
}

// Задача для потоков сортировки
class SortingTask implements Runnable {
    private final CustomLinkedList list;
    private final AtomicInteger totalSteps;
    private int localSteps = 0;

    public SortingTask(CustomLinkedList list, AtomicInteger totalSteps) {
        this.list = list;
        this.totalSteps = totalSteps;
    }

    @Override
    public void run() {
        while (true) {
            try {
                boolean swapped = list.bubbleSortStep();
                localSteps++;
                totalSteps.incrementAndGet();

                // Задержка между шагами
                Thread.sleep(1000);


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
