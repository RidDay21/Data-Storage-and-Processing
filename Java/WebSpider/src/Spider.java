import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class Spider {
    private final HttpClient client;
    private final String baseUrl;
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final Queue<String> toVisit = new ConcurrentLinkedQueue<>();
    private final List<PathMessage> results = new CopyOnWriteArrayList<>();

    private static class PathMessage implements Comparable<PathMessage> {
        final String path;
        final String message;

        PathMessage(String path, String message) {
            this.path = path;
            this.message = message;
        }

        @Override
        public int compareTo(PathMessage other) {
            return this.path.compareTo(other.path);
        }
    }

    public Spider(int port) {
        this.baseUrl = "http://localhost:" + port;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        List<String> successors = new ArrayList<>();

        try {
            // Упрощенный парсинг JSON
            int messageStart = json.indexOf("\"message\":\"");
            if (messageStart != -1) {
                int start = messageStart + 10; // длина "\"message\":\""
                int end = json.indexOf("\"", start + 1);
                if (end != -1) {
                    String message = json.substring(start + 1, end);
                    result.put("message", message);
                }
            }

            int successorsStart = json.indexOf("\"successors\":[");
            if (successorsStart != -1) {
                int start = successorsStart + 14; // длина "\"successors\":["
                int end = json.indexOf("]", start);
                if (end != -1) {
                    String arrayContent = json.substring(start, end);
                    // Убираем пробелы и кавычки
                    arrayContent = arrayContent.replace("\"", "").replace(" ", "");
                    if (!arrayContent.isEmpty()) {
                        String[] items = arrayContent.split(",");
                        for (String item : items) {
                            if (!item.isEmpty()) {
                                successors.add(item);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка парсинга JSON: " + e.getMessage());
        }

        result.put("successors", successors);
        return result;
    }

    private void processPath(String path) {
        try {
            // Формируем правильный URL
            String urlPath = path.startsWith("/") ? path : "/" + path;
            URI uri = URI.create(baseUrl + urlPath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> data = parseJson(response.body());
                String message = (String) data.get("message");
                @SuppressWarnings("unchecked")
                List<String> successors = (List<String>) data.get("successors");

                results.add(new PathMessage(path, message != null ? message : ""));
                System.out.printf("Обработан путь: %s, сообщение: %s%n",
                        path, message != null ? message : "");

                if (successors != null) {
                    for (String successor : successors) {
                        if (visited.add(successor)) {
                            toVisit.add(successor);
                        }
                    }
                }
            } else {
                System.err.printf("HTTP ошибка для пути %s: %d%n", path, response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обработке " + path + ": " + e.getMessage());
        }
    }

    public void crawl() throws Exception {
        System.out.println("Запуск паука для сервера на порту: " +
                baseUrl.substring(baseUrl.lastIndexOf(":") + 1));

        toVisit.add("/");
        visited.add("/");

        // Используем виртуальные потоки (Java 21+)
        ExecutorService executor;
        try {
            executor = Executors.newVirtualThreadPerTaskExecutor();
        } catch (Exception e) {
            // Fallback для более старых версий Java
            executor = Executors.newFixedThreadPool(50);
        }

        List<Future<?>> futures = new ArrayList<>();

        while (true) {
            String path = toVisit.poll();
            if (path != null) {
                Future<?> future = executor.submit(() -> processPath(path));
                futures.add(future);
            } else {
                // Проверяем, есть ли еще задачи в обработке
                Thread.sleep(50);

                // Если очередь пуста и все задачи завершены
                boolean allDone = true;
                for (Future<?> future : futures) {
                    if (!future.isDone()) {
                        allDone = false;
                        break;
                    }
                }

                if (allDone && toVisit.isEmpty()) {
                    break;
                }

                // Проверяем не слишком часто
                if (toVisit.isEmpty()) {
                    Thread.sleep(200);
                }
            }
        }

        executor.shutdown();
        if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
            executor.shutdownNow();
        }

        Collections.sort(results);
        printResults();
    }

    private void printResults() {
        System.out.println("\n=== Результаты (отсортированы лексикографически) ===");
        for (PathMessage pm : results) {
            System.out.printf("%s: %s%n", pm.path, pm.message);
        }
        System.out.printf("\nВсего обработано: %d путей%n", results.size());
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Использование: java Spider <port>");
            System.out.println("Пример: java Spider 8080");
            System.exit(1);
        }

        try {
            int port = Integer.parseInt(args[0]);
            Spider spider = new Spider(port);
            spider.crawl();
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}