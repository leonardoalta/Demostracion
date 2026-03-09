package connectionFramework.server;

import connectionFramework.dao.PersonaDao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final PersonaDao dao;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.dao = new PersonaDao();
    }

    @Override
    public void run() {
        System.out.println("Cliente conectado desde: " + socket.getInetAddress().getHostAddress());

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String request = in.readLine();
            if (request == null || request.isBlank()) {
                out.println("ERROR|Petición vacía");
                return;
            }

            String response = process(request);
            out.println(response);

        } catch (Exception e) {
            System.out.println("Error atendiendo cliente: " + e.getMessage());
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