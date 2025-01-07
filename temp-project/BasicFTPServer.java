import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class BasicFTPServer {
    public static void main(String[] args) {
        int port = 21; // Default FTP port
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("FTP Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client in a new thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Send welcome message
            writer.println("220 Welcome to My FTP Server");

            String command;
            while ((command = reader.readLine()) != null) {
                System.out.println("Received: " + command);

                if (command.startsWith("USER")) {
                    writer.println("331 User name okay, need password.");
                } else if (command.startsWith("PASS")) {
                    writer.println("230 User logged in, proceed.");
                } else if (command.startsWith("LIST")) {
                    writer.println("150 Opening ASCII mode data connection for file list.");
                    // Send directory listing
                    writer.println("testfile.txt");
                    writer.println("anotherfile.txt");
                    writer.println("226 Transfer complete.");
                } else {
                    writer.println("502 Command not implemented.");
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
