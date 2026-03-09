package connectionFramework.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CrudClient {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 5050;

        try (
                BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Cliente conectado al servidor " + host + ":" + port);
            System.out.println("Ejemplos de peticiones:");
            System.out.println("CREATE|Juan|20");
            System.out.println("READ|1");
            System.out.println("UPDATE|1|Pedro|22");
            System.out.println("DELETE|1");
            System.out.println("LIST");
            System.out.print("Escribe la petición: ");

            String request = teclado.readLine();

            try (
                    Socket socket = new Socket(host, port);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                out.println(request);
                String response = in.readLine();
                System.out.println("Respuesta del servidor: " + response);
            }

        } catch (Exception e) {
            System.out.println("Error en cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }
}