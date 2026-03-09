        package connectionFramework.factory;

        import java.sql.Connection;
        import java.sql.SQLException;

        /**
         * Interfaz para crear conexiones.
         * Patrón aplicado: Factory Method
         * - El pool NO debe saber cómo se construye una conexión.
         * - Se desacopla la creación del uso.
         */
        public interface ConnectionFactory {
            Connection create() throws SQLException;
        }