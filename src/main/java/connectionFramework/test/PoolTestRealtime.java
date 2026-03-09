package connectionFramework.test;

import connectionFramework.config.DbPoolConfig;
import connectionFramework.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.*;

public final class PoolTestRealtime {

    public static void main(String[] args) throws Exception {

        DbPoolConfig cfg = DbPoolConfig.builder()
                .jdbcUrl("jdbc:postgresql://localhost:5432/escuela")
                .user("postgres")
                .password("postgres")
                .minSize(2)
                .maxSize(500)
                .acquireTimeout(Duration.ofSeconds(2))
                .build();

        ConnectionPool pool = ConnectionPool.init(cfg);

        int workers = 150;
        int tasks = 30000;

        ExecutorService exec = Executors.newFixedThreadPool(workers);
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();

        // Monitor del pool cada 500ms
        monitor.scheduleAtFixedRate(() -> {
            System.out.println("MONITOR -> " + pool.snapshot());
        }, 0, 5000, TimeUnit.MILLISECONDS);

        Random rnd = new Random();
        CountDownLatch latch = new CountDownLatch(tasks);

        for (int i = 0; i < tasks; i++) {
            int taskId = i + 1;

            exec.submit(() -> {
                try {
                    int sleepSec = 1 + rnd.nextInt(3);

                    try (Connection c = pool.acquire();
                         PreparedStatement ps = c.prepareStatement("SELECT pg_sleep(?)")) {

                        ps.setInt(1, sleepSec);
                        ps.execute();
                    }

                } catch (Exception e) {
                    System.out.println("TASK #" + taskId + " -> ERROR: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        exec.shutdown();
        monitor.shutdownNow();

        System.out.println("\n=== FINAL ===");
        System.out.println(pool.snapshot());

        pool.shutdown();
    }
}