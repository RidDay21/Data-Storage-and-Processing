package ru.nsu.laptev;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class KeyClient {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java KeyClient <host> <port> <name> [delay] [exitBeforeReading]");
            return;
        }

        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            String name = args[2];
            int delay = args.length > 3 ? Integer.parseInt(args[3]) : 0;
            System.out.println("Вот наш Делэй:" + delay);
            boolean exitEarly = args.length > 4 && Boolean.parseBoolean(args[4]);

            requestKeys(host, port, name, delay, exitEarly);
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void requestKeys(String host, int port, String name, int delay, boolean exitEarly)
            throws Exception {
        // Валидация имени
        for (char c : name.toCharArray()) {
            if (c > 0x7F) {
                System.err.println("Error: name must contain only ASCII characters");
                return;
            }
        }

        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            // Отправка имени
            out.write(name.getBytes(StandardCharsets.US_ASCII));
            out.write(0);
            out.flush();
            System.out.println("Sent client name: " + name);

            if (delay > 0) {
                System.out.println("⏳ Delaying for " + delay + " seconds...");
                Thread.sleep(delay * 1000L);
                System.out.println("✅ Delay finished, starting to read response...");
            }


            //БИ-ДОУ-БИ-ДОУ
            if (exitEarly) {
                return;
            }

            // Чтение ответа
            String response = readResponse(in);
            saveKeys(name, response);
            System.out.println("✅ Keys saved for: " + name);

        }
    }

    private static String readResponse(InputStream in) throws IOException {
        StringBuilder response = new StringBuilder();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, StandardCharsets.US_ASCII));

        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append('\n');
        }
        return response.toString();
    }

    private static void saveKeys(String name, String pemData) throws IOException {
        if (pemData == null || pemData.trim().isEmpty()) {
            System.err.println("Error: Empty response from server");
            return;
        }

        // Парсинг PEM блоков
        String privBegin = "-----BEGIN PRIVATE KEY-----";
        String privEnd   = "-----END PRIVATE KEY-----";
        String pubBegin  = "-----BEGIN PUBLIC KEY-----";
        String pubEnd    = "-----END PUBLIC KEY-----";
        String certBegin = "-----BEGIN CERTIFICATE-----";
        String certEnd   = "-----END CERTIFICATE-----";

        int privStart = pemData.indexOf(privBegin);
        int privStop  = pemData.indexOf(privEnd);
        int pubStart  = pemData.indexOf(pubBegin);
        int pubStop   = pemData.indexOf(pubEnd);
        int certStart = pemData.indexOf(certBegin);
        int certStop  = pemData.indexOf(certEnd);

        if (privStart < 0 || privStop < 0 || pubStart < 0 || pubStop < 0 || certStart < 0 || certStop < 0) {
            throw new IOException("Invalid response from server: missing PEM blocks");
        }

        // Извлекаем полные PEM блоки
        String privPem = pemData.substring(privStart, privStop + privEnd.length()).trim() + "\n";
        String pubPem  = pemData.substring(pubStart, pubStop + pubEnd.length()).trim() + "\n";
        String certPem = pemData.substring(certStart, certStop + certEnd.length()).trim() + "\n";

        // Сохраняем в файлы
        writeFile(name + ".key", privPem);
        writeFile(name + ".pub", pubPem);
        writeFile(name + ".crt", certPem);
    }

    private static void writeFile(String filename, String content) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(content);
        }
    }
}