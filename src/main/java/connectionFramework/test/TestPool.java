package connectionFramework.test;

import connectionFramework.config.DbPoolConfig;
import connectionFramework.pool.ConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestPool {
    public static void main(String[] args) throws Exception {

        DbPoolConfig cfg = DbPoolConfig.builder()
                .jdbcUrl("jdbc:postgresql://localhost:5432/escuela")
                .user("postgres")
                .password("postgres")
                .minSize(2)
                .maxSize(10)
                .build();

        ConnectionPool pool = ConnectionPool.init(cfg);

        System.out.println("created=" + pool.snapshot() + " available=" + pool.snapshot());

        // try-with-resources llama close(), y close() regresa al pool (Proxy)
        try (Connection c = pool.acquire();
             PreparedStatement ps = c.prepareStatement("SELECT now()");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            System.out.println("DB time = " + rs.getTimestamp(1));
        }

        System.out.println("after query: inUse=" + pool.snapshot() + " available=" + pool.snapshot());

        pool.shutdown();
    }
}