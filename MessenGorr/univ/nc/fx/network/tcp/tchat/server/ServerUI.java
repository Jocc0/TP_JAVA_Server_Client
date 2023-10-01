import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * Interface graphique du
 *
 * @author mathieu.fabre
 */
public class ServerUI extends Application {

    /**
     * Indique si le serveur tourne
     */
    private boolean running = false;
    private Server server;

    /**
     * Bouton d'action pour lancer ou stoper le serveur
     */
    private Button run;
    private Button stop;

    /**
     * Champs pour choisir l'ip et le port
     */
    private TextField ip;
    private TextField port;

    /**
     * Zone de log et status
     */
    private static TextArea textArea;
    private Label status;

    /**
     * Indique si le serveur tourne
     *
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Ajoute un log dans la fenetre de log
     * 
     * @param message
     */
    public static void log(String message) {
        textArea.appendText(System.getProperty("line.separator") + message);
    }

    /**
     * Demarrage de l'interface graphique
     *
     * @param stage
     * @throws Exception
     */
    public void start(Stage stage) throws Exception {

        // Creation du layout principal
        BorderPane borderPane = new BorderPane();
        Scene mainScene = new Scene(borderPane);
        stage.setScene(mainScene);
        stage.setWidth(800.0);
        stage.setHeight(600.0);

        // Creation de la toolbar
        ToolBar toolBar = new ToolBar();
        // Créez un champ de saisie pour l'adresse IP
        Label labelIp = new Label("IP : ");
        ip = new TextField("127.0.0.1");
        Label labelPort = new Label("Port : ");
        port = new TextField("6699");
        run = new Button("Démarrer le server");
        run.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String IpServer = ip.getText();
                startServer(IpServer);
            }

        });
        stop = new Button("Arrêter le server");
        stop.setOnAction(new EventHandler<ActionEvent>() {
            @Override

            public void handle(ActionEvent event) {
                stopServer();
            }

        });
        toolBar.getItems().addAll(labelIp, ip, labelPort, port, run, stop);

        // Personnalisatiion de la barre d'outils
        toolBar.setStyle("-fx-background-color: #0084FF;");
        labelIp.setTextFill(javafx.scene.paint.Color.WHITE);
        labelPort.setTextFill(javafx.scene.paint.Color.WHITE);
        run.setStyle("-fx-background-color: #00A200; -fx-text-fill: white;");
        stop.setStyle("-fx-background-color: #FF0000; -fx-text-fill: white;");

        // Creation du logger
        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        textArea.setWrapText(true);

        textArea.setStyle("-fx-background-color: #F0F0F0;");
        textArea.setPrefHeight(400.0);

        // Creation de la zone de status
        status = new Label("Prêt...");
        status.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        status.setTextFill(javafx.scene.paint.Color.DARKSEAGREEN);

        status.setStyle("-fx-background-color: #F0F0F0;");
        status.setTextAlignment(TextAlignment.CENTER);

        borderPane.setTop(toolBar);
        borderPane.setCenter(textArea);
        borderPane.setBottom(status);

        // Initialisation de l etat de l'IHM
        setNonRunningState();

        stage.setTitle("Servor");
        stage.show();
    }

    /**
     * Demarrage du serveur
     */
    public void startServer(String ipAddress) {

        // Changement de l etat de 'IHM
        setRunningState();

        // Changement de l etat du server
        running = true;

        String serverIp = ip.getText().trim();
        int ServerPort = Integer.parseInt(port.getText().trim());

        server = new Server(ServerPort, serverIp, this);
        server.start();
    }

    /**
     * Mets l'IHM dans l etat running
     */
    public void setRunningState() {
        ip.setDisable(true);
        port.setDisable(true);
        run.setDisable(true);
        stop.setDisable(false);
        textArea.setDisable(false);
        status.setText("En cours d'exécution");
    }

    /**
     * Mets l'IHM dans l etat non running
     */
    public void setNonRunningState() {
        ip.setDisable(false);
        port.setDisable(false);
        run.setDisable(false);
        stop.setDisable(true);
        textArea.setDisable(true);
        status.setText("Prêt");
    }

    /**
     * Arret du server
     * On change simplement le statut
     * et le serveur s'arrete tout seul
     */
    public void stopServer() {
        setNonRunningState();
        // On marque l'arret et on attends l'arret du server
        running = false;
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
