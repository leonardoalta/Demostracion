package connectionFramework.strategy;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Estrategia de validación.
 * Patrón aplicado: Strategy
 * - Permite cambiar la política de validación sin modificar el pool.
 * - Ejemplos: isValid(timeout), SELECT 1, o validación más compleja.
 */
public interface ConnectionValidator {
    boolean isValid(Connection c) throws SQLException;
}