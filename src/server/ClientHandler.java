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
     * Envia uma mensagem para todos os clientes conectados ao servidor, exceto para
     * o cliente que a enviou.
     * 
     * O método permite a adição de um prefixo, dependendo se o cliente é um jogador
     * ou espectador, para personalizar a mensagem. Caso um erro ocorra durante o
     * envio
     * da mensagem, a conexão com o cliente é encerrada.
     * 
     * @param message   A mensagem a ser enviada para todos os clientes, exceto o
     *                  que a enviou.
     * @param addPrefix Se {@code true}, adiciona um prefixo indicando se o cliente
     *                  é um jogador ou espectador.
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
     * Envia uma mensagem para todos os clientes conectados ao servidor.
     * 
     * Este método percorre a lista de clientes conectados e envia uma mensagem para
     * cada um,
     * prefixando com "🖥️:" para indicar que é uma mensagem do servidor. Caso
     * ocorra um erro ao
     * enviar a mensagem, a conexão com o cliente é encerrada.
     * 
     * @param message Mensagem a ser enviada para todos os clientes.
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
     * Envia uma mensagem exclusivamente para o cliente conectado.
     * A mensagem é precedida pelo ícone 🖥️ e é enviada ao cliente por meio do
     * writer.
     * 
     * @param message A mensagem a ser enviada ao cliente.
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
     * Verifica se o cliente é um jogador ou um espectador, com base em sua posição
     * na lista de clientes.
     * 
     * O método considera que os primeiros {@code MAX_PLAYERS} clientes na lista são
     * jogadores,
     * e os demais são espectadores.
     * 
     * @return {@code true} se o cliente for um jogador, {@code false} se for um
     *         espectador.
     */
    public boolean isPlayer() {
        return clients.indexOf(this) < MAX_PLAYERS; // Verifica se é um jogador ou espectador.
    }

    /**
     * Informa que um cliente se conectou ao servidor e é adicionado à lista de
     * usuários conectados.
     * 
     * Envia uma mensagem ao cliente informando seu papel (jogador ou espectador) e
     * oferece a lista de comandos.
     * Em seguida, transmite uma mensagem a todos os clientes conectados,
     * notificando sobre a entrada do usuário,
     * seja como jogador ou espectador.
     */
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
     * Remove o cliente da lista de clientes conectados e notifica os outros
     * clientes
     * sobre a desconexão do usuário.
     * 
     * A mensagem de saída do usuário é enviada a todos os clientes conectados,
     * e um log é gerado no servidor.
     */
    public void userLeft() {
        clients.remove(this); // Remove o jogador da lista de conectados.

        broadcastMessage("🖥️: " + username + " saiu do servidor 😔!", false);
        System.out.println(username + " saiu do servidor!"); // Log no servidor.
    }

    /**
     * Encerra a conexão do client e fecha os objetos reader e writer.
     * Este método é responsável por fechar a conexão com o client, encerrando os
     * fluxos
     * de entrada e saída de dados (reader e writer) e o socket de comunicação.
     * 
     * @param socket O socket de conexão que será fechado.
     * @param reader O BufferedReader usado para ler dados do client.
     * @param writer O BufferedWriter usado para enviar dados ao client.
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
     * Lida com os comandos executados pelos jogadores (não espectadores).
     * Este método processa comandos como "/pedra", "/papel", "/tesoura", e "/jogar"
     * para definir a escolha do jogador ou iniciar a partida.
     * 
     * @param command O comando executado pelo jogador. Pode ser "/pedra", "/papel",
     *                "/tesoura", ou "/jogar".
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
     * Verifica se existem dois jogadores no servidor, contando quantos
     * clientes estão registrados como jogadores.
     * 
     * @return true se houver exatamente dois jogadores no servidor,
     *         false caso contrário.
     */
    private boolean hasTwoPlayers() {
        int playerCount = 0;

        for (ClientHandler client : clients) {
            if (client.isPlayer()) {
                playerCount++;
            }
        }

        return playerCount == 2; // Retornando a quantidade de clients conectados.
    }

    /**
     * Valida as escolhas dos jogadores e chama o método para calcular o resultado
     * da partida.
     * <p>
     * Este método realiza as seguintes validações:
     * <ul>
     * <li>Verifica se há exatamente dois jogadores conectados ao servidor;</li>
     * <li>Verifica se o jogador atual já fez sua escolha de jogada;</li>
     * <li>Verifica se o outro jogador também fez sua escolha de jogada.</li>
     * </ul>
     * Caso todas as condições sejam atendidas, o método chama o método
     * {@link #calculateResult()} para calcular o resultado da partida.
     */
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
     * Calcula o resultado de uma partida de Jokenpo entre dois jogadores.
     * 
     * O método compara as escolhas dos dois jogadores (armazenadas em
     * `playerChoice`) e determina o vencedor
     * com base nas regras do jogo Jokenpo (Pedra, Papel e Tesoura). Se ambos os
     * jogadores escolherem a mesma opção,
     * o resultado é um empate. Caso contrário, o vencedor é determinado conforme as
     * regras do jogo.
     * 
     * Após determinar o vencedor ou o empate, o método envia uma mensagem para o
     * servidor informando o resultado
     * e então reinicia o jogo.
     * 
     * @see {@link #resetGame()} Para reiniciar o jogo após calcular o resultado.
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

        // Verifica quem ganhou:
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

    /**
     * Embaralha a lista de clientes, redefinindo os dois primeiros como jogadores e
     * os demais como espectadores.
     * Reseta as escolhas dos jogadores e envia mensagens para todos os clientes
     * informando suas funções.
     */
    public void resetGame() {
        // Embaralha a lista de clientes.
        Collections.shuffle(clients);

        // Itera sobre a lista de clientes.
        for (int i = 0; i < clients.size(); i++) {
            ClientHandler client = clients.get(i);

            if (i < 2) { // Os dois primeiros clientes serão jogadores.
                client.playerChoice = null; // Reseta a escolha do jogador.
                client.sendMessageToClient("Você agora é um jogador. Escolha sua jogada.");
            } else { // Os outros clientes serão espectadores.
                client.sendMessageToClient("Você é um espectador.");
            }
        }

        // Envia uma mensagem para todos os clientes (jogadores e espectadores).
        sendServerMessage("\nO jogo foi reiniciado! Escolham suas jogadas.");
    }

    /**
     * Aguarda por mensagens do cliente e processa comandos ou transmite mensagens.
     * 
     * O método executa um loop enquanto a conexão do cliente estiver ativa. Ele
     * aguarda mensagens do cliente,
     * processa comandos específicos (como /sair, /comandos, ou jogadas de pedra,
     * papel e tesoura),
     * e as transmite para outros clientes ou processa as ações de jogador conforme
     * apropriado.
     * 
     * Se o cliente enviar uma mensagem de desconexão ou ocorrer um erro de leitura,
     * a conexão é encerrada.
     */
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
