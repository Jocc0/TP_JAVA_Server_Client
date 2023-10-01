import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Client de tchat
 */
public class Client extends Thread implements ITchat {

	private SocketChannel sc;
	private Selector selector;
	private ByteBuffer buffer;
	private String nickname;
	private ClientUI clientUI;
	private ArrayList<String> messageBuffer;

	// Le constructeur prend en arguments
	public Client(ClientUI clientUI, String IpServer, int port, String nickname) {

		this.clientUI = clientUI;
		this.nickname = nickname;

		try {
			// Crée et configure le SocketChannel
			sc = SocketChannel.open();
			sc.configureBlocking(false);

			sc.connect(new InetSocketAddress(IpServer, port));
			while (!sc.finishConnect()) {

			}
			// Crée et configure le Selector
			selector = Selector.open();
			sc.register(selector, SelectionKey.OP_READ);

			buffer = ByteBuffer.allocate(BUFFER_SIZE);
			messageBuffer = new ArrayList<>();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			// Tant que l'interface du client est en cours il continue l'exécution
			while (clientUI.isRunning()) {

				// Sélectione les canaux qui seront prêts à être lus
				selector.select();
				// Obtient les clés selectionnées
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				// Iterateur pour le parcours des clés selectionnées
				Iterator<SelectionKey> KeyIterator = selectedKeys.iterator();

				// Ici il parcourt tant qu'il y a des clés selectionnées
				while (KeyIterator.hasNext()) {
					SelectionKey key = KeyIterator.next();
					KeyIterator.remove();

					if (key.isConnectable()) {
						// Gère la connexion
						handleConnect(key);
					} else if (key.isReadable()) {
						// Gère la lecture
						handleRead(key);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Permet de fermer le channel
	public void disconnect() {
		try {
			sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleConnect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		if (channel.isConnectionPending()) {
			channel.finishConnect();
		}
		channel.register(selector, SelectionKey.OP_READ);
	}

	private void handleRead(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		buffer.clear();
		int bytesRead = channel.read(buffer);
		if (bytesRead == -1) {
			// Le server a coupé la connexion
			channel.close();
			return;
		}
		// Prépare le buffer à la lecture
		buffer.flip();
		String message = new String(buffer.array(), 0, bytesRead);
		messageBuffer.add(message); // Ajoute le message au buffer
		clientUI.appendMessage(message);
	}

	// Cette méthode sert pour récupérer les messages du messageBuffer
	public ArrayList<String> getMessages() {
		return messageBuffer;
	}

	// Ici la méthode sendMessages sert pour envoyer un message au Serveur
	public void sendMessage(String message) throws IOException {
		String messageWithUsername = nickname + ": " + message;
		ByteBuffer messageBuffer = ByteBuffer.wrap(messageWithUsername.getBytes());
		sc.write(messageBuffer);
	}
}
