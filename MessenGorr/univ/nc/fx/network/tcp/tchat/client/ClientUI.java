
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Interface graphique du client
 *
 * @author mathieu.fabre
 */
public class ClientUI extends Application {

    private TextField ip;
    private TextField port;
    private TextField nickname;
    private Button connect;
    private Button disconnect;
    private TextArea textArea;
    private TextField input;
    private Label status;

    /**
     * Le thread client
     */
    private Client client;

    /**
     * Indique si le client tourne
     */
    private boolean running = false;

    public void start(Stage stage) throws Exception {

        // Border pane et scene
        BorderPane borderPane = new BorderPane();
        Scene scene = new Scene(borderPane);
        stage.setScene(scene);

        // ZOne haute pour la connection
        ToolBar toolBar = new ToolBar();
        ip = new TextField("127.0.0.1");
        port = new TextField("6699");
        nickname = new TextField("user" + (new Random().nextInt(100)));
        nickname.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    connectToServer();
                }
            }
        });
        connect = new Button("Connect");
        connect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                connectToServer();
            }
        });
        disconnect = new Button("Disconnect");
        disconnect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                disconnectFromServer();
            }
        });

        toolBar.getItems().addAll(ip, port, nickname, connect, disconnect);
        borderPane.setTop(toolBar);

        // Personnalisatiion de la barre d'outils
        toolBar.setStyle("-fx-background-color: #0084FF;");
        connect.setStyle("-fx-background-color: #00A200; -fx-text-fill: white;");
        disconnect.setStyle("-fx-background-color: #FF0000; -fx-text-fill: white;");

        // Creation du logger
        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        textArea.setWrapText(true);

        textArea.setStyle("-fx-background-color: #F0F0F0;");
        textArea.setPrefHeight(400.0);

        // Creation de la zone de status
        status = new Label("Pret");
        status.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        status.setTextFill(javafx.scene.paint.Color.DARKSEAGREEN);

        status.setStyle("-fx-background-color: #F0F0F0;");
        status.setTextAlignment(TextAlignment.CENTER);

        // Zone centrale de log de tchat
        textArea = new TextArea();
        borderPane.setCenter(textArea);

        // ZOne basse pour la xone de texte et le statut
        VBox bottomBox = new VBox();
        status = new Label("Pret");
        input = new TextField();
        input.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                processEnter(event);
            }
        });
        bottomBox.getChildren().addAll(input, status);
        borderPane.setBottom(bottomBox);

        // Statut initial deconnecte
        setDisconnectedState();
        setStatus("Client déconnecté :(");

        stage.setTitle("Messengor");
        stage.show();
    }

    /**
     * Mets l'IHM dans le staut deconnecte
     */
    public void setDisconnectedState() {
        ip.setDisable(false);
        port.setDisable(false);
        connect.setDisable(false);
        disconnect.setDisable(true);
        input.setDisable(true);
    }

    public void setConnectedState() {
        ip.setDisable(true);
        port.setDisable(true);
        connect.setDisable(true);
        disconnect.setDisable(false);
        input.setDisable(false);
    }

    /**
     * Indique si le client tourne
     *
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Ajout de message dans le log
     *
     * @param message
     */
    public void appendMessage(String message) {
        // Permet d'avoir l'heure sur le tchat :D
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = currentTime.format(formatter);

        textArea.appendText("[" + formattedTime + "] " + message + "\n");
        textArea.positionCaret(textArea.getText().length());
    }

    /**
     * Change le ;essage de statut
     *
     * @param message
     */
    public void setStatus(String message) {
        status.setText(message);
    }

    /**
     * Connexion au serveur
     */
    public void connectToServer() {

        if (ip.getText().trim().length() == 0) {
            setStatus("Veuillez entrer une adresse IP valide");
            return;
        }

        if (port.getText().trim().length() == 0) {
            setStatus("Veuillez entrer un port valide");
            return;
        }

        if (nickname.getText().trim().length() == 0) {
            setStatus("Veuillez entrer un nickname valide");
            return;
        }

        String ServerIp = ip.getText().trim();
        int ServerPort = Integer.parseInt(port.getText().trim());
        String username = nickname.getText().trim();
        try {

            client = new Client(this, ServerIp, ServerPort, username);

            client.start();

            // Changement de l etat du client
            running = true;

            // Changement d etat de l'IHM
            setConnectedState();
            setStatus("Client connecté :)");

        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Erreur de connexion au serveur" + e.getMessage());
        }
    }

    /**
     * Deconnexion
     * on passe le statut a false et on attends
     * que le thread se deconnecte
     */
    public void disconnectFromServer() {
        running = false;
        // Force la fermeture du client s'il est ouvert
        if (client != null) {
            try {
                client.disconnect();
                client.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                ;
            }
            client = null;
        }
        // Mise à jour d'état de l'IHM
        setDisconnectedState();
        setStatus("Client déconnecté :(");
    }

    /**
     * Envoi le message si l utilisateur
     * appui sur la touche entree
     *
     * @param event
     */
    public void processEnter(KeyEvent event) {

        // Envoi du texte si on appui sur entree et que le contenu n est pas vide
        if (event.getCode() == KeyCode.ENTER && input.getText().trim().length() > 0) {
            try {
                String message = input.getText();
                if (!message.isEmpty()) {
                    client.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            input.setText("");
        }
    }

    /**
     * Demarrage du client
     *
     * @param args
     */
    public static void main(String[] args) {
        launch(args);
    }
}
