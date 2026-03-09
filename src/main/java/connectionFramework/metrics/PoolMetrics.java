package connectionFramework.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * PoolMetrics almacena estadísticas acumuladas del pool.
 * Estas métricas permiten observar si el pool está creando conexiones,
 * reutilizando conexiones disponibles, o saturándose (timeouts).
 */
public final class PoolMetrics {

    public final AtomicLong created = new AtomicLong(0);         // conexiones físicas creadas (histórico)
    public final AtomicLong borrowed = new AtomicLong(0);        // total de llamadas a acquire()
    public final AtomicLong returned = new AtomicLong(0);        // total de devoluciones (release)
    public final AtomicLong reused = new AtomicLong(0);          // veces que se obtuvo una conexión ya existente del pool
    public final AtomicLong createdOnDemand = new AtomicLong(0); // veces que se creó una conexión por falta de disponibles
    public final AtomicLong timeouts = new AtomicLong(0);        // acquire() falló por timeout

    /** Línea compacta para imprimir en el monitor. */
    public String snapshotLine() {
        return "metrics{created=" + created.get()
                + ", borrowed=" + borrowed.get()
                + ", returned=" + returned.get()
                + ", reused=" + reused.get()
                + ", createdOnDemand=" + createdOnDemand.get()
                + ", timeouts=" + timeouts.get()
                + "}";
    }
}