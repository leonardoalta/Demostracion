package connectionFramework.strategy;

import connectionFramework.config.DbPoolConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Validador por defecto:
 * - Si hay validationQuery (por defecto "SELECT 1"), la ejecuta.
 * - Si no hay validationQuery, usa Connection.isValid(timeout).
 */
public final class DefaultValidator implements ConnectionValidator {

    private final DbPoolConfig cfg;

    public DefaultValidator(DbPoolConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    public boolean isValid(Connection c) throws SQLException {
        int seconds = (int) Math.max(1, cfg.validationTimeout.toSeconds());

        // Opción A: usar isValid del driver
        if (cfg.validationQuery == null || cfg.validationQuery.isBlank()) {
            return c.isValid(seconds);
        }

        // Opción B: ejecutar una query simple (clásico en pools)
        try (Statement st = c.createStatement()) {
            st.setQueryTimeout(seconds);
            try (ResultSet rs = st.executeQuery(cfg.validationQuery)) {
                return rs.next();
            }
        }
    }
}