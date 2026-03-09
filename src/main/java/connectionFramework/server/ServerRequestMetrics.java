package connectionFramework.server;

import java.util.concurrent.atomic.AtomicLong;

public final class ServerRequestMetrics {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalProcessingNanos = new AtomicLong(0);

    public void recordSuccess(long nanos) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        totalProcessingNanos.addAndGet(nanos);
    }

    public void recordFailure(long nanos) {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
        totalProcessingNanos.addAndGet(nanos);
    }

    public String snapshotLine() {
        return "serverMetrics{totalRequests=" + totalRequests.get()
                + ", successful=" + successfulRequests.get()
                + ", failed=" + failedRequests.get()
                + ", avgProcessMs=" + String.format("%.3f", averageProcessingMillis())
                + "}";
    }

    public double averageProcessingMillis() {
        long total = totalRequests.get();
        if (total == 0) return 0.0;
        return (totalProcessingNanos.get() / 1_000_000.0) / total;
    }

    public String finalReport() {
        return "\n=== REPORTE FINAL DEL SERVIDOR ===\n"
                + "Solicitudes totales atendidas: " + totalRequests.get() + "\n"
                + "Solicitudes exitosas: " + successfulRequests.get() + "\n"
                + "Solicitudes fallidas: " + failedRequests.get() + "\n"
                + "Tiempo promedio de procesamiento: " + String.format("%.3f", averageProcessingMillis()) + " ms\n";
    }
}