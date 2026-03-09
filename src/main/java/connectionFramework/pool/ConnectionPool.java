package connectionFramework.pool;

import connectionFramework.config.DbPoolConfig;
import connectionFramework.factory.ConnectionFactory;
import connectionFramework.factory.PostgresConnectionFactory;
import connectionFramework.metrics.PoolMetrics;
import connectionFramework.strategy.ConnectionValidator;
import connectionFramework.strategy.DefaultValidator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ConnectionPool {

    private static volatile ConnectionPool INSTANCE;

    public static ConnectionPool init(DbPoolConfig cfg) throws SQLException {
        if (INSTANCE == null) {
            synchronized (ConnectionPool.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConnectionPool(cfg);
                }
            }
        }
        return INSTANCE;
    }

    public static ConnectionPool get() {
        ConnectionPool p = INSTANCE;
        if (p == null) {
            throw new IllegalStateException("Pool no inicializado. Llama primero a ConnectionPool.init(cfg).");
        }
        return p;
    }

    private final DbPoolConfig cfg;
    private final ConnectionFactory factory;
    private final ConnectionValidator validator;

    private final BlockingQueue<Connection> available;
    private final AtomicInteger createdNow = new AtomicInteger(0);
    private final AtomicInteger inUse = new AtomicInteger(0);

    private final PoolMetrics metrics = new PoolMetrics();

    private final AtomicLong idSeq = new AtomicLong(0);
    private final ConcurrentHashMap<Connection, Long> connIds = new ConcurrentHashMap<>();

    private final boolean logEvents = true;

    private ConnectionPool(DbPoolConfig cfg) throws SQLException {
        this.cfg = cfg;
        this.factory = new PostgresConnectionFactory(cfg);
        this.validator = new DefaultValidator(cfg);
        this.available = new LinkedBlockingQueue<>();

        for (int i = 0; i < cfg.minSize; i++) {
            available.offer(newPhysicalConnection(false));
        }
    }

    public PoolMetrics getMetrics() {
        return metrics;
    }

    public String snapshot() {
        return "pool{inUse=" + inUse.get()
                + ", available=" + available.size()
                + ", createdNow=" + createdNow.get()
                + ", max=" + cfg.maxSize
                + "} " + metrics.snapshotLine();
    }

    private Connection newPhysicalConnection(boolean onDemand) throws SQLException {
        int current = createdNow.incrementAndGet();
        if (current > cfg.maxSize) {
            createdNow.decrementAndGet();
            throw new SQLException("Se alcanzó maxSize del pool: " + cfg.maxSize);
        }

        Connection c = factory.create();
        c.setAutoCommit(true);

        long id = idSeq.incrementAndGet();
        connIds.put(c, id);

        metrics.created.incrementAndGet();
        if (onDemand) {
            metrics.createdOnDemand.incrementAndGet();
        }

        if (logEvents) {
            log("CREATE", id, onDemand ? "onDemand" : "preCreate");
        }

        return c;
    }

    private void closePhysical(Connection c) {
        long id = connIds.getOrDefault(c, -1L);

        try {
            c.close();
        } catch (SQLException ignored) {
        }

        connIds.remove(c);
        createdNow.decrementAndGet();

        if (logEvents && id >= 0) {
            log("CLOSE", id, "physical");
        }
    }

    private long idOf(Connection c) {
        return connIds.getOrDefault(c, -1L);
    }

    public Connection acquire() throws SQLException {
        metrics.borrowed.incrementAndGet();
        Duration t = cfg.acquireTimeout;

        try {
            Connection physical = available.poll(t.toMillis(), TimeUnit.MILLISECONDS);

            if (physical != null) {
                metrics.reused.incrementAndGet();
                long id = idOf(physical);
                if (logEvents) {
                    log("BORROW", id, "reused");
                }
            } else {
                if (createdNow.get() < cfg.maxSize) {
                    physical = newPhysicalConnection(true);
                    long id = idOf(physical);
                    if (logEvents) {
                        log("BORROW", id, "new");
                    }
                } else {
                    metrics.timeouts.incrementAndGet();
                    if (logEvents) {
                        log("TIMEOUT", -1, "noAvailableAndAtMax");
                    }
                    throw new SQLException("Timeout esperando conexión del pool.");
                }
            }

            if (!validator.isValid(physical)) {
                long id = idOf(physical);
                if (logEvents) {
                    log("INVALID", id, "replacing");
                }
                closePhysical(physical);
                physical = newPhysicalConnection(true);
            }

            inUse.incrementAndGet();
            return proxyConnection(physical);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrumpido esperando conexión.", e);
        }
    }

    void release(Connection physical) {
        long id = idOf(physical);

        try {
            if (!physical.getAutoCommit()) {
                physical.rollback();
                physical.setAutoCommit(true);
            }

            available.offer(physical);
            metrics.returned.incrementAndGet();

            if (logEvents) {
                log("RETURN", id, "ok");
            }

        } catch (SQLException e) {
            if (logEvents) {
                log("DISCARD", id, "releaseError");
            }
            closePhysical(physical);

        } finally {
            inUse.decrementAndGet();
        }
    }

    public void shutdown() {
        Connection c;
        while ((c = available.poll()) != null) {
            closePhysical(c);
        }
    }

    private Connection proxyConnection(Connection physical) {
        AtomicInteger closed = new AtomicInteger(0);

        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();

            if ("close".equals(methodName)) {
                if (closed.compareAndSet(0, 1)) {
                    release(physical);
                }
                return null;
            }

            if ("isClosed".equals(methodName)) {
                return closed.get() == 1;
            }

            if (closed.get() == 1) {
                throw new SQLException("La conexión proxy ya fue cerrada.");
            }

            try {
                return method.invoke(physical, args);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    private void log(String event, long id, String note) {
        String who = Thread.currentThread().getName();
        String time = LocalTime.now().toString();

        if (id >= 0) {
            System.out.printf("%s [%s] %-7s conn#%d (%s)%n", time, who, event, id, note);
        } else {
            System.out.printf("%s [%s] %-7s (%s)%n", time, who, event, note);
        }
    }
}