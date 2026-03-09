package connectionFramework.server;

import connectionFramework.config.DbPoolConfig;
import connectionFramework.pool.ConnectionPool;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CrudServer {

    public static void main(String[] args) {
        int port = 5050;

        int serverWorkerThreads = 160;
        int queueCapacity = 70000;

        ServerRequestMetrics requestMetrics = new ServerRequestMetrics();

        try {
            DbPoolConfig cfg = DbPoolConfig.builder()
                    .jdbcUrl("jdbc:postgresql://localhost:5432/escuela")
                    .user("postgres")
                    .password("postgres")
                    .minSize(15)
                    .maxSize(40)
                    .acquireTimeout(Duration.ofSeconds(8))
                    .validationTimeout(Duration.ofSeconds(2))
                    .validationQuery("SELECT 1")
                    .build();

            ConnectionPool pool = ConnectionPool.init(cfg);

            ThreadPoolExecutor workerPool = new ThreadPoolExecutor(
                    serverWorkerThreads,
                    serverWorkerThreads,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueCapacity),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
            monitor.scheduleAtFixedRate(() -> {
                System.out.println("MONITOR -> " + pool.snapshot() + " "
                        + requestMetrics.snapshotLine()
                        + " executor{active=" + workerPool.getActiveCount()
                        + ", queued=" + workerPool.getQueue().size()
                        + ", completed=" + workerPool.getCompletedTaskCount()
                        + "}");
            }, 0, 5, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nApagando servidor...");
                monitor.shutdownNow();
                workerPool.shutdown();
                try {
                    if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                        workerPool.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                MetricsReporter.printFinalReport(pool, requestMetrics);
                pool.shutdown();
            }));

            try (ServerSocket serverSocket = new ServerSocket(port, 1000)) {
                serverSocket.setReuseAddress(true);
                System.out.println("Servidor escuchando en puerto " + port);

                while (true) {
                    Socket client = serverSocket.accept();
                    workerPool.execute(new ClientHandler(client, requestMetrics));
                }
            }

        } catch (Exception e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}