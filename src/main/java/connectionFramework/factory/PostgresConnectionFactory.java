package connectionFramework.factory;

import connectionFramework.config.DbPoolConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory concreta para PostgreSQL.
 * Aquí se encapsula el uso de DriverManager y el JDBC URL.
 * Si mañana cambia el método de conexión (SSL, parámetros, etc.),
 * se modifica aquí y el pool no cambia.
 */
public final class PostgresConnectionFactory implements ConnectionFactory {

    private final DbPoolConfig cfg;

    public PostgresConnectionFactory(DbPoolConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public Connection create() throws SQLException {
        return DriverManager.getConnection(cfg.jdbcUrl, cfg.user, cfg.password);
    }
}