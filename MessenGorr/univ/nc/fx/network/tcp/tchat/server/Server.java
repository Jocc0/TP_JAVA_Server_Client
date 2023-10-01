import javafx.application.Application;
import javafx.application.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Processus serveur qui ecoute les connexion entrantes,
 * les messages entrant et les rediffuse au clients connectes
 *
 * @author mathieu.fabre
 */
public class Server extends Thread implements ITchat {

    private ServerSocketChannel scc;
    private Selector selector;
    private ServerUI serverUI;
    private String Ip;
    private ByteBuffer buffer;
    private Set<SocketChannel> clients;

    // Constructeur avec le port et l'interface
    public Server(int port, String ipAdress, ServerUI serverUI) {

        this.serverUI = serverUI;
        this.Ip = ipAdress;

        try {

            // Création du ServerSocketChannel
            scc = ServerSocketChannel.open();
            scc.bind(new InetSocketAddress(ipAdress, port));
            scc.configureBlocking(false);

            // Création du Selector
            selector = Selector.open();
            // Enregistrement du scc sur le Selector pour accepter les connexions entrantes
            scc.register(selector, SelectionKey.OP_ACCEPT);

            // Initialisation du buffer avec la size récupérer sur ITchat
            buffer = ByteBuffer.allocate(BUFFER_SIZE);

            // Initialisation de la liste des clients connectés au serveur
            clients = new HashSet<>();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            while (serverUI.isRunning()) {

                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {

                    SelectionKey key = keyIterator.next();
                    // Permet
                    keyIterator.remove();

                    if (key.isAcceptable()) {
                        // Gère les nouvelles connexions
                        handleAcceptable(key);
                    } else if (key.isReadable()) {
                        // Gère la lecture des messages des clients connectés
                        handleRead(key);
                    }

                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cette méthode permet de gérer les nouvelles connexions
    private void handleAcceptable(SelectionKey key) {
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);

            // Enregistrez le canal client pour les opérations de lecture
            clientChannel.register(selector, SelectionKey.OP_READ);

            // Ici j'ai rajouté le client à la liste des clients connectés
            clients.add(clientChannel);

            // Message de bienvenue automatique au client
            String bienvenido = "Bienvenue sur le serveur !";
            sendToClient(clientChannel, bienvenido);
            // Envoyez un message de journal à l'interface utilisateur
            sendLogToUI("Nouvelle connexion client: " + clientChannel.getRemoteAddress());
            /**
             * Envoi un message de log a l'IHM
             */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cette méthode permet de gérer la lecture des messages des clients du serveur
    private void handleRead(SelectionKey key) {

        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            buffer.clear();
            int bytesRead = clientChannel.read(buffer);

            if (bytesRead == -1) {
                // Le client a été déconnecté du serveur
                handleDisconnect(clientChannel);
                return;
            }
            buffer.flip();
            String message = new String(buffer.array(), 0, bytesRead);

            // Utilise la méthode broadcastMessage afin d'envoyer le message à tous les
            // utilisateurs
            broadcastMessage(message);

            // Affiche le message sur l'interface du Serveur
            sendLogToUI("Message reçu de " + clientChannel.getRemoteAddress() + ": " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cette méthode gère la déconnexion d'un client du serveur
    private void handleDisconnect(SocketChannel clientChannel) {
        try {

            // Remove le client de la liste des clients en ligne
            clients.remove(clientChannel);

            // Envoyer un message sur la déconnexion du client
            sendLogToUI("Déconnexion de : " + clientChannel.getRemoteAddress());

            // Fermer le channel du client
            clientChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cette méthode permet d'envoyer un message à un client précis
    private void sendToClient(SocketChannel clientChannel, String message) {
        try {
            ByteBuffer messageBuffer = ByteBuffer.wrap(message.getBytes());
            clientChannel.write(messageBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Cette méthode permet de diffuser un message à tous les clients présent
    private void broadcastMessage(String message) throws IOException {

        // Pour tous les clients dans la liste des clients envoyer le message
        for (SocketChannel clientChannel : clients) {
            sendToClient(clientChannel, message);
        }

    }

    public void sendLogToUI(String message) {
        Platform.runLater(() -> ServerUI.log(message));
    }

}
