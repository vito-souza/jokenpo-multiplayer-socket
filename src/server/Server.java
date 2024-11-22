package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    /** Socket utilizado para instânciar um servidor. */
    private ServerSocket serverSocket;

    // Construtor para receber um Server Socket:
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /** Inicia um servidor na rede. */
    public void runServer() {
        try {
            // Mantém o server aberto até que o host o desligue:
            while (!serverSocket.isClosed()) {
                System.out.println("Servidor iniciado na porta: " + serverSocket.getLocalPort());
                Socket socket = serverSocket.accept(); // Aceita a conexão de um jogador ao server.

                ClientHandler clientHandler = new ClientHandler(socket); // Instância um novo handler.
                System.out.println(clientHandler.getUsername() + " entrou na partida!");

                // Instância uma nova thread para cada jogador:
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            closeServer(); // Caso de erro, fecha o servidor.
        }
    }

    /** Encerra o servidor. */
    public void closeServer() {
        try {
            serverSocket.close(); // Encerrando o servidor.
        } catch (IOException e) {
            e.printStackTrace(); // Pilha de execução.
        }
    }

    /**
     * Instânca e roda um novo servidor.
     * 
     * @param args
     */
    public static void main(String[] args) throws IOException {
        Server server = new Server(new ServerSocket(7777)); // Instânciando um novo servidor.
        server.runServer(); // Rodando o servidor.
    }
}