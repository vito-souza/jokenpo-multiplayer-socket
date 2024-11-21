package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    /** Socket passado pela classe server. Utilizado para estabelecer conexão. */
    private Socket socket;

    /** Utilizado para ler dados/mensagens enviadas pelo server. */
    private BufferedReader reader;

    /** Utilizado para enviar dados/mensagens para o server. */
    private BufferedWriter writer;

    private String username;

    // Construtor:
    public Client(Socket socket, String username) {
        try {
            this.socket = socket;

            // Converte o stream de bits em caractéres.
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            this.username = username;
        } catch (IOException e) {
            closeConnection(socket, reader, writer); // Encerrando a conexão com o server.
        }
    }

    /**
     * Método responsável por enviar mensagens para o servidor.
     */
    public void sendMessage() {
        try {
            writer.write(username); // Para que o ClientHandler possa identificar.
            writer.newLine();
            writer.flush();

            Scanner scanner = new Scanner(System.in); // Para receber entradas pelo console.

            // Enquanto o usuário estiver conectado no server:
            while (socket.isConnected()) {
                String message = scanner.nextLine(); // Recebendo a entrada do usuário.

                writer.write(username + ": " + message); // Imprimindo a entrada do usuário no server.
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            closeConnection(socket, reader, writer); // Encerrando a conexão.
        }
    }

    /**
     * Método responsável por receber mensagens do servidor. Age como thread para
     * não comprometer o funcionamento da aplicação.
     */
    public void listenForMessage() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // Mensagem enviada no chat.
                String serverMessage;

                // Enquanto houver conexão:
                while (socket.isConnected()) {
                    try {
                        serverMessage = reader.readLine(); // Lendo a mensagem enviada no chat.

                        // Se não estiver recebendo mais mensagens.
                        if (serverMessage == null) {
                            closeConnection(socket, reader, writer); // Encerra as conexões.
                            System.out.println("Conexão encerrada com o host.");
                            break; // Sai do loop.
                        }

                        System.out.println(serverMessage); // Imprimindo a mensagem no console.
                    } catch (IOException e) {
                        closeConnection(socket, reader, writer); // Encerrando a conexão.
                        break; // Encerra o loop.
                    }
                }
            }
        }).start();
    }

    /**
     * Encerra a conexão do client e fecha os objetos reader e writer.
     * 
     * @param socket Socket de conexão.
     * @param reader BufferedReader.
     * @param writer BufferedWriter.
     */
    public void closeConnection(Socket socket, BufferedReader reader, BufferedWriter writer) {
        // Fechando o reader, writer e socket.
        try {
            if (reader != null)
                reader.close();

            if (writer != null)
                writer.close();

            if (socket != null)
                socket.close(); // Fechar o socket também encerra o Output/InputStreamWriter.
        } catch (IOException e) {
            e.printStackTrace(); // Imprimindo a pilha da exception no console.
        }
    }

    /**
     * Se conecta a um servidor.
     * 
     * @param args
     * @throws IOException
     * @throws UnknownHostException
     */
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in); // Scanner para receber entradas do usuário.

        System.out.print("Insira um nome de usuário: ");

        String username = scanner.nextLine(); // Nome de usuário.
        Socket socket = new Socket("localhost", 7070); // Socket de conexão com o server.

        Client client = new Client(socket, username); // Instânciando um novo client.

        client.listenForMessage(); // Recebe mensagens vindas do server.
        client.sendMessage(); // Envia mensagens para o server.
    }
}