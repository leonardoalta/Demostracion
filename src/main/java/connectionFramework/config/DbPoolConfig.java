    package connectionFramework.config;

    import java.time.Duration;
    import java.util.Objects;

    /**
     * DbPoolConfig representa la configuraci&oacute;n del pool de conexiones.
     * Patron aplicado: Builder
     * - Evita constructores con muchos par&aacute;metros.
     * - Permite configurar de forma legible y segura valores opcionales.
     */
    public final class DbPoolConfig {

        // Datos de conexión
        public final String jdbcUrl;
        public final String user;
        public final String password;

        // Tamaños del pool
        public final int minSize;
        public final int maxSize;

        // Timeouts importantes
        public final Duration acquireTimeout;     // cuánto esperar por una conexión
        public final Duration validationTimeout;  // cuánto esperar en la validación

        // Query opcional para validar conexiones (ej. SELECT 1)
        public final String validationQuery;

        private DbPoolConfig(Builder b) {
            this.jdbcUrl = b.jdbcUrl;
            this.user = b.user;
            this.password = b.password;
            this.minSize = b.minSize;
            this.maxSize = b.maxSize;
            this.acquireTimeout = b.acquireTimeout;
            this.validationTimeout = b.validationTimeout;
            this.validationQuery = b.validationQuery;
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder para construir DbPoolConfig.
         */
        public static final class Builder {
            private String jdbcUrl;
            private String user;
            private String password;

            private int minSize = 2;
            private int maxSize = 10;

            private Duration acquireTimeout = Duration.ofSeconds(5);
            private Duration validationTimeout = Duration.ofSeconds(2);

            private String validationQuery = "SELECT 1";

            public Builder jdbcUrl(String v) { this.jdbcUrl = v; return this; }
            public Builder user(String v) { this.user = v; return this; }
            public Builder password(String v) { this.password = v; return this; }

            public Builder minSize(int v) { this.minSize = v; return this; }
            public Builder maxSize(int v) { this.maxSize = v; return this; }

            public Builder acquireTimeout(Duration v) { this.acquireTimeout = v; return this; }
            public Builder validationTimeout(Duration v) { this.validationTimeout = v; return this; }

            public Builder validationQuery(String v) { this.validationQuery = v; return this; }

            public DbPoolConfig build() {
                // Validaciones básicas (fail fast)
                Objects.requireNonNull(jdbcUrl, "jdbcUrl");
                Objects.requireNonNull(user, "user");
                Objects.requireNonNull(password, "password");

                if (minSize < 0 || maxSize <= 0 || minSize > maxSize) {
                    throw new IllegalArgumentException("minSize/maxSize inválidos");
                }
                Objects.requireNonNull(acquireTimeout, "acquireTimeout");
                Objects.requireNonNull(validationTimeout, "validationTimeout");

                return new DbPoolConfig(this);
            }
        }
    }