package connectionFramework.server;

import connectionFramework.pool.ConnectionPool;

public final class MetricsReporter {

    private MetricsReporter() {
    }

    public static void printFinalReport(ConnectionPool pool, ServerRequestMetrics serverMetrics) {
        System.out.println(serverMetrics.finalReport());
        System.out.println(pool.getMetrics().finalReport());
        System.out.println("Estado final del pool: " + pool.snapshot());
    }
}