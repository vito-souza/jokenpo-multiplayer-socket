package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
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
     * Envia uma mensagem para todos os clients conectados ao servidor, exceto para
     * o client que a enviou.
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
     * Envia uma mensagem para todos os clients conectados ao servidor.
     * 
     * @param message Mensagem a ser enviada para todos os clients.
     */
    public void sendServerMessage(String message) {
        // Percorre a lista de usuários conectados.
        clients.forEach(client -> {
            try {
                // Envia a mensagem para todos os clientes conectados.
                client.writer.write("🖥️: " + message); // Envia a mensagem.
                client.writer.newLine(); // Adiciona uma nova linha.
                client.writer.flush(); // Garante que a mensagem seja enviada.
            } catch (IOException e) {
                closeConnection(socket, reader, writer); // Encerra a conexão se houver erro.
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
        if (!command.equals("/jogar") && playerChoice != null) {
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
            case "/jogar":
                play();
                return; // Sai do método.
            default:
                sendMessageToClient("Comando inválido! Digite /comandos para uma lista de comandos válidos.");
                return; // Sai do método em caso de comando inválido.
        }

        sendMessageToClient("Você escolheu " + playerChoice + ".");
        broadcastMessage("🖥️: " + username + " escolheu sua jogada.", false);
    }

    /**
     * Verifica se existem dois jogadores no servidor.
     */
    private boolean hasTwoPlayers() {
        int playerCount = 0;

        for (ClientHandler client : clients) {
            if (client.isPlayer()) {
                playerCount++;
            }
        }

        return playerCount == 2;
    }

    /** Valida as escolhas dos jogadors e chama o método de calcular resultado */
    public void play() {
        // Verifica se há dois jogadores no servidor
        if (!hasTwoPlayers()) {
            sendMessageToClient("Aguardando outro jogador conectar para iniciar a partida.");
            return;
        }

        // Verifica se o jogador atual já escolheu sua jogada
        if (playerChoice == null) {
            sendMessageToClient("Você ainda não escolheu sua jogada.");
            return;
        }

        // Verifica se o outro jogador fez sua escolha
        for (ClientHandler client : clients) {
            if (!client.equals(this) && client.isPlayer() && client.playerChoice == null) {
                sendMessageToClient(client.username + " ainda não fez sua jogada.");
                return;
            }
        }

        // Calcula o resultado da partida
        calculateResult();
    }

    /**
     * Cálcula o resultado de uma partida.
     */
    public void calculateResult() {
        // Identificando os jogadores:
        ClientHandler player1 = clients.get(0);
        ClientHandler player2 = clients.get(1);

        // Verifica se ambos fizeram suas escolhas
        if (player1.playerChoice == null || player2.playerChoice == null)
            return; // Não processa se as escolhas não forem feitas.

        // Se os dois jogadores escolheram a mesma coisa:
        if (player1.playerChoice.equals(player2.playerChoice)) {
            sendServerMessage("Ambos os jogadores escolheram " + player1.playerChoice + ". Empate 🔥!");
            return; // Encerra o método.
        }

        if ((player1.playerChoice.equals("pedra") && player2.playerChoice.equals("tesoura")) ||
                (player1.playerChoice.equals("tesoura") && player2.playerChoice.equals("papel")) ||
                (player1.playerChoice.equals("papel") && player2.playerChoice.equals("pedra"))) {
            sendServerMessage(
                    player1.username + " venceu! (" + player1.playerChoice + " vs. " + player2.playerChoice + ") 🔥!");
        } else {
            sendServerMessage(
                    player2.username + " venceu! (" + player2.playerChoice + " vs. " + player1.playerChoice + ") 🔥!");
        }

        resetGame(); // Reseta o jogo.
    }

    /** Reseta o jogo */
    public void resetGame() {
        // Embaralha a lista de clientes
        Collections.shuffle(clients);

        // Itera sobre a lista de clientes
        for (int i = 0; i < clients.size(); i++) {
            ClientHandler client = clients.get(i);

            if (i < 2) { // Os dois primeiros clientes serão jogadores
                client.playerChoice = null; // Reseta a escolha do jogador
                client.sendMessageToClient("Você agora é um jogador. Escolha sua jogada.");
            } else { // Os outros clientes serão espectadores
                client.sendMessageToClient("Você é um espectador.");
            }
        }

        // Envia uma mensagem para todos os clientes (jogadores e espectadores)
        sendServerMessage("O jogo foi reiniciado! Escolham suas jogadas.");
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

                    // Se o client utilizou o comando de sair:
                    if (message.startsWith("/sair")) {
                        closeConnection(socket, reader, writer); // Encerra a conexão.
                        break; // Encerra o loop.
                    }

                    // Se o client deseja ver a lista de comandos:
                    if (message.startsWith("/comandos")) {
                        sendMessageToClient("Comandos disponíveis: /comandos, /sair, /pedra, /papel e /tesoura.");
                        continue;
                    }

                    // Se for um jogador e não executou os comandos acima, verifica a entrada do
                    // comando:
                    if (isPlayer())
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
