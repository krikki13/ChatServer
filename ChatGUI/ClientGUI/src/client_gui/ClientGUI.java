package client_gui;

import java.io.IOException;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * JavaFX graphical interface that uses {@link server_api.Client} for amazing control
 * and convenience when communicating with a server.
 * 
 * @author KRIKKI
 * @since 11. 7. 2017
 * @version 1.0
 */
public class ClientGUI extends Application {
    private Scene clientScene;
    private Stage stage;
    private final String version = "1.0";
    
    /**
     * A zero argument constructor. Not much to see here.
     */
    public ClientGUI(){}
    
    /**
     * A start method that is overriden from Application and is started by Application API.
     * It sets up the stage for {@link ClientGUIController}.
     * 
     * @param primaryStage This is the stage that is created, when program is run.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client_gui_controller.fxml"));
            Parent setupParent = loader.load();
            
            clientScene = new Scene(setupParent);
            
            stage = primaryStage;

            stage.setScene(clientScene);
            stage.setMinWidth(480);
            stage.setMinHeight(400);
            stage.show();
            stage.setTitle("Client GUI "+version);
            
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    Platform.exit();
                    System.exit(0);
                }
            });
        } catch (IOException ex) {
            System.out.println("An error has occured!");
            ex.printStackTrace();
        }
    }

    /**
     * The good ol' main method.
     * 
     * @param args This is... well what do you think?
     */
    public static void main(String[] args) {
        launch(args);
    }
    
   
    
}
