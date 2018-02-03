package server_gui;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * JavaFX graphical interface that uses {@link server_api.Server} for amazing control
 * and convenience when creating a local server.
 * 
 * @author KRIKKI
 * @since 6. 7. 2017
 * @version 1.0
 */
public class ServerGUI extends Application {
    private ServerGUIController controller;
    private Scene serverScene;
    private Stage stage;
    final String version = "1.0";
    
    /**
     * A start method that is overriden from Application and is started by Application API.
     * It sets up the stage for {@link ServerGUIController}.
     * 
     * @param primaryStage This is the stage that is created, when program is run.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server_gui_controller.fxml"));
            Parent setupParent = loader.load();
            
            controller=(ServerGUIController)loader.getController();          

            serverScene = new Scene(setupParent);
            //serverScene.getStylesheets().add("/spacesim/styles.css");
            
            stage = primaryStage;

            stage.setScene(serverScene);
            stage.setMinWidth(480);
            stage.setMinHeight(400);
            stage.show();
            stage.setTitle("Server GUI "+version);
            controller.setParent(this);
            
            primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent t) {
                    try{
                        controller.server.stop();
                    }catch(Exception e){}
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
