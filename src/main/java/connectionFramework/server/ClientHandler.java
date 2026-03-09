package connectionFramework.server;

import connectionFramework.dao.PersonaDao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final PersonaDao dao;
    private final ServerRequestMetrics metrics;

    public ClientHandler(Socket socket, ServerRequestMetrics metrics) {
        this.socket = socket;
        this.dao = new PersonaDao();
        this.metrics = metrics;
    }

    @Override
    public void run() {
        long start = System.nanoTime();

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String request = in.readLine();

            if (request == null || request.isBlank()) {
                out.println("ERROR|Petición vacía");
                metrics.recordFailure(System.nanoTime() - start);
                return;
            }

            String response = process(request);
            out.println(response);

            if (response.startsWith("OK|")) {
                metrics.recordSuccess(System.nanoTime() - start);
            } else {
                metrics.recordFailure(System.nanoTime() - start);
            }

        } catch (Exception e) {
            metrics.recordFailure(System.nanoTime() - start);
        } finally {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String process(String request) {
        try {
            String[] parts = request.split("\\|");
            String action = parts[0].toUpperCase();

            switch (action) {
                case "CREATE":
                    if (parts.length != 3) return "ERROR|Formato esperado: CREATE|nombre|edad";
                    return dao.create(parts[1], Integer.parseInt(parts[2]));

                case "READ":
                    if (parts.length != 2) return "ERROR|Formato esperado: READ|id";
                    return dao.read(Integer.parseInt(parts[1]));

                case "UPDATE":
                    if (parts.length != 4) return "ERROR|Formato esperado: UPDATE|id|nombre|edad";
                    return dao.update(Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]));

                case "DELETE":
                    if (parts.length != 2) return "ERROR|Formato esperado: DELETE|id";
                    return dao.delete(Integer.parseInt(parts[1]));

                case "LIST":
                    return dao.list();

                default:
                    return "ERROR|Operación no válida";
            }

        } catch (NumberFormatException e) {
            return "ERROR|Número inválido en la petición";
        } catch (Exception e) {
            return "ERROR|" + e.getMessage();
        }
    }
}