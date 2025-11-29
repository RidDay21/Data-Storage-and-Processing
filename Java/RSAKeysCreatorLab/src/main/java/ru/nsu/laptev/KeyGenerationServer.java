package ru.nsu.laptev;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;

public class KeyGenerationServer {
    private final int port;
    private final ExecutorService workerPool;
    private final ConcurrentMap<String, CompletableFuture<KeyData>> cache = new ConcurrentHashMap<>();
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();

    private final X500Name issuer;
    private final PrivateKey caPrivateKey;
    private final KeyPairGenerator keyGen;

    public KeyGenerationServer(int port, int threads, String issuerName, PrivateKey caPrivateKey)
            throws NoSuchAlgorithmException {
        this.port = port;
        this.workerPool = Executors.newFixedThreadPool(threads);
        this.issuer = new X500Name(issuerName);
        this.caPrivateKey = caPrivateKey;

        this.keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(8192);

        // Запускаем worker threads
        for (int i = 0; i < threads; i++) {
            workerPool.submit(this::worker);
        }
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(() -> handleClient(clientSocket));
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket;
             InputStream input = clientSocket.getInputStream();
             OutputStream output = clientSocket.getOutputStream()) {

            String name = readName(input);
            if (name == null || name.isEmpty()) return;

            System.out.println("Request for: " + name);

            KeyData keys = getOrGenerateKeys(name).get();
            sendResponse(output, keys);

            System.out.println("Sent keys for: " + name);

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private String readName(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = input.read()) != 0 && b != -1) {
            if (b > 0x7F) continue;
            buffer.write(b);
            if (buffer.size() > 4096) throw new IOException("Name too long");
        }
        return buffer.toString("US-ASCII");
    }

    private CompletableFuture<KeyData> getOrGenerateKeys(String name) {
        return cache.computeIfAbsent(name, key -> {
            CompletableFuture<KeyData> future = new CompletableFuture<>();
            queue.offer(new Task(name, future));
            return future;
        });
    }

    private void worker() {
        while (true) {
            try {
                Task task = queue.take();
                processTask(task);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void processTask(Task task) {
        try {
            KeyPair keyPair = keyGen.generateKeyPair();
            X509Certificate cert = generateCertificate(task.name(), keyPair);
            task.future().complete(new KeyData(keyPair, cert));
        } catch (Exception e) {
            task.future().completeExceptionally(e);
            cache.remove(task.name(), task.future());
        }
    }

    private X509Certificate generateCertificate(String name, KeyPair keyPair) throws Exception {
        X500Name subject = new X500Name("CN=" + name);
        BigInteger serial = new BigInteger(64, new SecureRandom());

        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 365L * 24 * 60 * 60 * 1000);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .build(caPrivateKey);
        //7
        return new JcaX509CertificateConverter().getCertificate(certBuilder.build(signer));
    }

    private void sendResponse(OutputStream output, KeyData keys) throws Exception {
        String response = keys.toPem();
        output.write(response.getBytes("US-ASCII"));
        output.flush();
    }

    public static PrivateKey loadPrivateKey(String filename) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(filename))) {
            Object object = parser.readObject();
            return new JcaPEMKeyConverter().getPrivateKey(
                    (org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java KeyGenerationServer <port> <threads> <issuer> <caKey>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);
        String issuer = args[2];
        PrivateKey caKey = loadPrivateKey(args[3]);

        new KeyGenerationServer(port, threads, issuer, caKey).start();
    }
}

record Task(String name, CompletableFuture<KeyData> future) {}

