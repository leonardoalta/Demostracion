package connectionFramework.metrics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class PoolMetrics {

    public final AtomicLong created = new AtomicLong(0);
    public final AtomicLong borrowed = new AtomicLong(0);
    public final AtomicLong returned = new AtomicLong(0);
    public final AtomicLong reused = new AtomicLong(0);
    public final AtomicLong createdOnDemand = new AtomicLong(0);
    public final AtomicLong timeouts = new AtomicLong(0);

    // Nuevas métricas
    public final AtomicLong totalAcquireWaitNanos = new AtomicLong(0);
    public final AtomicLong successfulAcquires = new AtomicLong(0);
    public final AtomicInteger peakInUse = new AtomicInteger(0);

    public void recordAcquireWait(long nanos) {
        totalAcquireWaitNanos.addAndGet(nanos);
        successfulAcquires.incrementAndGet();
    }

    public void updatePeakInUse(int currentInUse) {
        peakInUse.accumulateAndGet(currentInUse, Math::max);
    }

    public double reusePercentage() {
        long borrowedCount = borrowed.get();
        if (borrowedCount == 0) return 0.0;
        return (reused.get() * 100.0) / borrowedCount;
    }

    public double averageAcquireWaitMillis() {
        long ok = successfulAcquires.get();
        if (ok == 0) return 0.0;
        return (totalAcquireWaitNanos.get() / 1_000_000.0) / ok;
    }

    public String snapshotLine() {
        return "metrics{created=" + created.get()
                + ", borrowed=" + borrowed.get()
                + ", returned=" + returned.get()
                + ", reused=" + reused.get()
                + ", createdOnDemand=" + createdOnDemand.get()
                + ", timeouts=" + timeouts.get()
                + ", avgWaitMs=" + String.format("%.3f", averageAcquireWaitMillis())
                + ", reusePct=" + String.format("%.2f", reusePercentage())
                + ", peakInUse=" + peakInUse.get()
                + "}";
    }

    public String finalReport() {
        return "\n=== REPORTE FINAL DEL POOL ===\n"
                + "Conexiones creadas: " + created.get() + "\n"
                + "Solicitudes acquire(): " + borrowed.get() + "\n"
                + "Conexiones devueltas: " + returned.get() + "\n"
                + "Reutilizaciones: " + reused.get() + "\n"
                + "Creaciones bajo demanda: " + createdOnDemand.get() + "\n"
                + "Eventos de saturación (timeouts): " + timeouts.get() + "\n"
                + "Porcentaje de reutilización: " + String.format("%.2f", reusePercentage()) + "%\n"
                + "Tiempo promedio de espera para obtener conexión: " + String.format("%.3f", averageAcquireWaitMillis()) + " ms\n"
                + "Pico de conexiones activas: " + peakInUse.get() + "\n";
    }
}