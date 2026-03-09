package connectionFramework.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTestClient {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5050;
        int totalRequests = args.length > 2 ? Integer.parseInt(args[2]) : 10000;
        int concurrentClients = args.length > 3 ? Integer.parseInt(args[3]) : 60;
        String clientName = args.length > 4 ? args[4] : "CLIENT";
        int baseReadId = args.length > 5 ? Integer.parseInt(args[5]) : 1;

        ExecutorService exec = Executors.newFixedThreadPool(concurrentClients);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicLong ok = new AtomicLong();
        AtomicLong fail = new AtomicLong();
        AtomicLong totalNanos = new AtomicLong();

        System.out.println("=== INICIO DE PRUEBA ===");
        System.out.println("Cliente: " + clientName);
        System.out.println("Host: " + host + ":" + port);
        System.out.println("Peticiones: " + totalRequests);
        System.out.println("Concurrencia: " + concurrentClients);
        System.out.println();

        for (int i = 1; i <= totalRequests; i++) {
            final int id = i;

            exec.submit(() -> {
                long start = System.nanoTime();

                try (
                        Socket socket = new Socket(host, port);
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
                ) {
                    String request = buildRequest(clientName, id, baseReadId);

                    out.println(request);
                    String response = in.readLine();

                    if (response != null && response.startsWith("OK|")) {
                        ok.incrementAndGet();
                    } else {
                        fail.incrementAndGet();
                    }

                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    totalNanos.addAndGet(System.nanoTime() - start);
                    latch.countDown();
                }
            });
        }

        latch.await();
        exec.shutdown();

        long total = ok.get() + fail.get();
        double avgMs = total == 0 ? 0.0 : (totalNanos.get() / 1_000_000.0) / total;

        System.out.println("\n=== REPORTE DEL CLIENTE DE CARGA ===");
        System.out.println("Cliente: " + clientName);
        System.out.println("Peticiones enviadas: " + totalRequests);
        System.out.println("Éxitos: " + ok.get());
        System.out.println("Fallos: " + fail.get());
        System.out.println("Tiempo promedio por petición: " + String.format("%.3f", avgMs) + " ms");
    }

    private static String buildRequest(String clientName, int id, int baseReadId) {
        // Mezcla optimizada:
        // 70% READ, 20% CREATE, 10% UPDATE
        int mode = id % 10;

        if (mode <= 6) {
            int readId = baseReadId + (id % 200);
            return "READ|" + readId;
        } else if (mode <= 8) {
            return "CREATE|" + clientName + "_Persona_" + id + "|" + (18 + (id % 40));
        } else {
            int updateId = baseReadId + (id % 200);
            return "UPDATE|" + updateId + "|" + clientName + "_Edit_" + id + "|" + (20 + (id % 30));
        }
    }
}