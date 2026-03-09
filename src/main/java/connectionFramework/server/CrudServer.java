package connectionFramework.server;

import connectionFramework.config.DbPoolConfig;
import connectionFramework.pool.ConnectionPool;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrudServer {

    public static void main(String[] args) {
        int port = 5050;

        try {
            DbPoolConfig cfg = DbPoolConfig.builder()
                    .jdbcUrl("jdbc:postgresql://localhost:5432/escuela")
                    .user("postgres")
                    .password("postgres")
                    .minSize(2)
                    .maxSize(5)
                    .acquireTimeout(Duration.ofSeconds(5))
                    .validationTimeout(Duration.ofSeconds(2))
                    .validationQuery("SELECT 1")
                    .build();

            ConnectionPool.init(cfg);

            ExecutorService poolClientes = Executors.newCachedThreadPool();

            Thread monitor = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        System.out.println("MONITOR -> " + ConnectionPool.get().snapshot());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            monitor.setDaemon(true);
            monitor.start();

            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Servidor escuchando en puerto " + port);

                while (true) {
                    Socket client = serverSocket.accept();
                    poolClientes.execute(new ClientHandler(client));
                }
            }

        } catch (Exception e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}