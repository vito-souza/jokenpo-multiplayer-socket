package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Trata a conexão com clients. Instâncias são executadas em threads separadas.
 */
public class ClientHandler implements Runnable {

    /** Monitora e guarda os clients conectados ao servidor. */
    public static List<ClientHandler> clients = new ArrayList<>();

    /** Utilizado para estabelecer uma conexão com o server. */
    private Socket socket;

    /** Utilizado para ler dados/mensagens enviadas pelo client. */
    private BufferedReader reader;

    /** Utilizado para enviar dados/mensagens pelo server. */
    private BufferedWriter writer;

    /** Número máximo de jogadores por partida. */
    private static final int MAX_PLAYERS = 2;

    /** Jogada efetuada pelo usuário. */
    private String playerChoice;

    private String username;

    public String getUsername() {
        return username;
    }

    // Construtor:
    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;

            // Convertem os streams de bytes em streams de char.
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.username = reader.readLine(); // Recebe o username do client.
            userJoined(); // Adiciona o usuário ao servidor.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia uma mensagem para todos os clients conectados ao servidor.
     * 
     * @param message Mensagem a ser enviada no server.
     */
    public void broadcastMessage(String message, boolean addPrefix) {
        String playerStatus = addPrefix ? ((isPlayer()) ? "[jogador] " : "[espectador] ") : "";

        // Percorre a lista de usuários conectados.
        clients.forEach(client -> {
            try {
                // Não envia a mensagem para si mesmo.
                if (!client.username.equals(username)) {
                    String formatted = playerStatus + message; // Preenche a mensagem com o prefixo do jogador.

                    client.writer.write(formatted); // Imprime a mensagem para o client.
                    client.writer.newLine(); // Para de aguardar por uma mensagem.
                    client.writer.flush();
                }
            } catch (IOException e) {
                closeConnection(socket, reader, writer); // Encerra a conexão.
            }
        });
    }

    /**
     * Verifica se o client é um jogador ou um espectador.
     * 
     * @return {@code true} se for um jogador, {@code false} se for um espectador.
     */
    public boolean isPlayer() {
        return clients.indexOf(this) < MAX_PLAYERS; // Verifica se é um jogador ou espectador.
    }

    /** Informa que um client se conectou ao servidor. */
    public void userJoined() {
        clients.add(this); // Adiciona o client à lista de usuários conectados.
        sendMessageToClient("Você entrou como " + ((isPlayer()) ? "jogador." : "espectador.")
                + " Digite /comandos para a lista de comandos 😉.");

        if (isPlayer())
            broadcastMessage("🖥️: " + username + " entrou na partida 🎮!", false);
        else
            broadcastMessage("🖥️: " + username + " está assistindo à partida 👀!", false);
    }

    /**
     * Remove e informa que um client se desconectou do servidor.
     */
    public void userLeft() {
        clients.remove(this); // Remove o jogador da lista de conectados.

        broadcastMessage("🖥️: " + username + " saiu do servidor 😔!", false);
        System.out.println(username + " saiu do servidor!"); // Log no servidor.
    }

    /**
     * Envia uma mensagem exclusivamente para o cliente.
     * 
     * @param message Mensagem a ser enviada.
     */
    private void sendMessageToClient(String message) {
        try {
            writer.write("🖥️: " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Encerra a conexão do client e fecha os objetos reader e writer.
     * 
     * @param socket Socket de conexão.
     * @param reader BufferedReader.
     * @param writer BufferedWriter.
     */
    public void closeConnection(Socket socket, BufferedReader reader, BufferedWriter writer) {
        userLeft(); // Informa os usuários conectados.

        // Encerra a conexão e o reader e writer:
        try {
            if (reader != null)
                reader.close();

            if (writer != null)
                writer.close();

            if (socket != null)
                socket.close(); // Encerra o socket e o Output/InputStreamWriter.
        } catch (IOException e) {
            e.printStackTrace(); // Pilha de execução.
        }
    }

    /**
     * 
     * Lida com os comandos executados pelos jogadores (não espectadores).
     * 
     * @param command Comando sendo executado pelo player.
     */
    public void playerCommand(String command) {
        if (playerChoice != null) {
            sendMessageToClient("Você já escolheu " + playerChoice + ".");
            return; // Sai da função.
        }

        // Processando os comandos do usuário:
        switch (command.trim()) {
            case "/pedra":
                playerChoice = "pedra";
                break;
            case "/papel":
                playerChoice = "papel";
                break;
            case "/tesoura":
                playerChoice = "tesoura";
                break;
            case "/comandos":
                break;
            default:
                sendMessageToClient("Comando inválido! Digite /comandos para uma lista de comandos válidos.");
                return; // Sai do método em caso de comando inválido
        }

        sendMessageToClient("Você escolheu " + playerChoice + ".");
        broadcastMessage("🖥️: " + username + " escolheu sua jogada.", false);
    }

    /** Aguarda por mensagens vindas do client. */
    @Override
    public void run() {
        String message; // Mensagem enviada pelo usuário.

        // Equanto o client ainda estiver conectado:
        while (socket.isConnected()) {
            try {
                message = reader.readLine(); // Recebe a mensagem do usuário.

                // Se não receber mais mensagens ou se o usuário sair:
                if (message == null) {
                    closeConnection(socket, reader, writer); // Encerra a conexão.
                    break; // Para de aguardar mensagens (sai do loop).
                }

                // Verifica se o player executou um comando.
                if (message.trim().contains("/")) {
                    String[] parts = message.split(":"); // Divide a mensagem no nome de usuário.
                    message = parts[1].trim(); // Mantém a parte após o nome de usuário e status.

                    if (message.startsWith("/sair")) {
                        closeConnection(socket, reader, writer); // Encerra a conexão.
                        break; // Encerra o loop.
                    }

                    if (message.startsWith("/comandos")) {
                        // Confirma a escolha para o jogador
                        sendMessageToClient("Comandos disponíveis: /comandos, /sair, /pedra, /papel e /tesoura.");
                        continue;
                    }

                    if (isPlayer()) // Se for um jogador, recebe entradas exclusivas.
                        playerCommand(message); // Trata as entradas.
                    else
                        sendMessageToClient("Você deve ser um jogador para executar este comando.");
                } else {
                    broadcastMessage(message, true); // Envia a mensagens para todos os usuários.
                }
            } catch (IOException e) {
                closeConnection(socket, reader, writer); // Encerra a conexão.
                break; // Para de aguardar mensages.
            }
        }
    }
}
