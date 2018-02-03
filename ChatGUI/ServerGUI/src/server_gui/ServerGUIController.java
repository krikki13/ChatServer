package server_gui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import server_api.Message;
import server_api.PrivateServerPreferences;
import server_api.Server;

/**
 * The JavaFX controller class for Server control graphical interface. It is created and
 * initialized by FXMLLoader in {@link ServerGUI}. It manages most graphical things
 * and interaction with {@link server_api.Server}.
 * 
 * @author KRIKKI
 * @since 6.7.2017
 * @version 1.0
 */
public class ServerGUIController implements Initializable {

    @FXML
    Label serverStatus;
    @FXML
    TextFlow textflow;
    @FXML
    ListView<String> userlist;
    @FXML
    Button banUser;
    @FXML
    Button buttonPreferences;
    @FXML
    Button buttonSendToAll;
    @FXML
    Button buttonSendToSelected;
    @FXML
    TextField sendTextField;
    @FXML
    Label sendToLabel, clientInfoLabel1, clientInfoLabel2;
    @FXML
    ScrollPane scrollpane1, scrollpane2;

    Server server;
    Thread serverThread;
    PreferencesController prefsController;
    Stage prefsStage;
    private Preferences localPreferences;
    private SimpleDateFormat timeStampFormat;
    ServerGUI serverGUI;
    private boolean autoLoad;
    private boolean autoRun;
    private String loadFile;

    /**
     * A zero argument constructor. Not much to see here.
     */
    public ServerGUIController(){}
    
    /**
     * This method is called when stage is drawn. It creates a {@link server_api.Server}
     * object, it overrides it's println and onConnection methods and loads 
     * {@link server_api.PrivateServerPreferences} from a file if one is specified.
     * It also sets the listener for changes on ListView.
     * 
     * @param url I have no idea what to do with that.
     * @param rb I also have no idea what to do with that.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        banUser.setDisable(true);
        banUser.setText("Start Server");
        buttonSendToSelected.setDisable(true);
        clientInfoLabel1.setText("");
        clientInfoLabel2.setText("");
        userlist.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        scrollpane1.setFitToWidth(true); 
        scrollpane2.setFitToWidth(true); 
        scrollpane1.setFitToHeight(true); 
        scrollpane2.setFitToHeight(true); 
        
                
        server = new Server() {
                @Override
                public void println(String s) {
                    show(s);
                }

                @Override
                public void errPrintln(String s) {
                    showError(s);
                }
                
                @Override
                public void onServerStarted(){
                    Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        serverStatus.setText("Running at " + server.getServerPort());
                        }
                    });
                }
                
                @Override
                public void onNewConnectionOpened(int port){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(server.isLoginRequired())
                                userlist.getItems().add("Port: "+port);
                            else
                                userlist.getItems().add(":"+port);
                        }
                    });
                }

                @Override
                public void onSuccessfulLogin(String username, int port){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(server.isLoginRequired()){
                                // renames the user from Port: XXXX to actual username
                                int index = userlist.getItems().indexOf("Port: "+port);
                                if( index >= 0){
                                    userlist.getItems().set(index, username); 
                                }
                                if(clientInfoLabel2.getText().endsWith(": "+port)){
                                    showClientInfo(username);
                                }
                            }
                        }
                    });
                }
                
                @Override
                public void onConnectionClosed(String username, int port, String description){
                    super.onConnectionClosed(username, port, description); // to still get that print
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            if(server.isLoginRequired())
                                userlist.getItems().remove(username.replace(":", "Port: "));
                            else
                                userlist.getItems().remove(username);
                            userlist.getSelectionModel().selectFirst();
                            
                            if(clientInfoLabel2.getText().endsWith(": "+port)){
                                clientInfoLabel1.setText("");
                                clientInfoLabel2.setText("");
                            }
                        }
                    });
                }
        };
        
        PrivateServerPreferences prefs = null;
        // AUTOMATICALLY LOAD IF autoLoad==true AND FILE SPECIFIED IN loadedFile EXISTS
        localPreferences = Preferences.userNodeForPackage(this.getClass());
        autoLoad = localPreferences.getBoolean("autoLoad", false);
        if(autoLoad){
            loadFile = localPreferences.get("loadedFile", "");
            autoRun = localPreferences.getBoolean("autoRun", false);
            File file = new File(loadFile);
            if(file.exists()){
                if(file.getName().endsWith("bsp")){
                    try {
                        prefs = PrivateServerPreferences.readFromBinaryFile(file.getAbsolutePath());
                        server.setPrivateServerPreferences(prefs);
                        banUser.setDisable(false);
                        timeStampFormat = new SimpleDateFormat(prefs.getTimeStampFormat());
                        show("[system]: Automatically loaded Preferences file \""+file.getAbsolutePath()+"\"");
                        serverStatus.setText("Preferences loaded, ready to start");
                    } catch (Exception ex) {
                        showError("Exception: "+ex.getMessage());
                    }
                }else if(file.getName().endsWith("tsp")){
                    try {
                        prefs = PrivateServerPreferences.readFromTextFile(file.getAbsolutePath());
                        server.setPrivateServerPreferences(prefs);
                        banUser.setDisable(false);
                        timeStampFormat = new SimpleDateFormat(prefs.getTimeStampFormat());
                        show("[system]: Automatically loaded Preferences file \""+file.getAbsolutePath()+"\"");
                        serverStatus.setText("Preferences loaded, ready to start");
                    } catch (Exception ex) {
                        showError("Exception: "+ex.getMessage());
                    }
                }
            }
            // AUTORUN
            if(prefs != null && autoRun && prefs.isValid()){
                serverThread = new Thread(server);
                serverThread.start();
                buttonPreferences.setText("Stop Server");
                banUser.setText("Ban Selected User");
   
            }
        }else{
            banUser.setDisable(true);
            loadFile = "";
            autoRun = false;
        }
     
            
            
        userlist.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                int numOfSelected = userlist.getSelectionModel().getSelectedIndices().size();
                if(numOfSelected == 0){
                    banUser.setDisable(true);
                    buttonSendToSelected.setDisable(true);
                }else{
                    //int numOfSelected = userlist.getSelectionModel().getSelectedIndices().size();
                    banUser.setDisable(false);
                    buttonSendToSelected.setDisable(false);
                    if(numOfSelected == 1){
                        banUser.setText("Ban Selected User");
                        buttonSendToSelected.setText("Send To Selected User");
                        showClientInfo(userlist.getSelectionModel().getSelectedItem());
                    }else{
                        banUser.setText("Ban "+numOfSelected+" Selected Users");
                        buttonSendToSelected.setText("Send to "+numOfSelected+" Selected Users");
                    }
                }
            }
        });
  
    }
    
    /**
     * This method is meant to be called by parent class after initalization, so that
     * controller has access to parent's methods.
     * 
     * @param serverGUI This is the parent of this class;
     */
    public void setParent(ServerGUI serverGUI){
        this.serverGUI = serverGUI;

    }
    
    /**
     * This method is called when banUser button is clicked.
     * At the beginning it's text is set to "Start Server" (it will be disabled if no preferences have
     * been loaded automatically). Once pressed, it will change to "Ban Selected User" and 
     * if user is selected, pressing this button will disconnect him.
     */
    @FXML
    public void onBanUserClicked(){
        if(banUser.getText().equals("Start Server")){
            banUser.setDisable(true);
            banUser.setText("Ban Selected User");
            serverThread = new Thread(server);
            serverThread.start();
            buttonPreferences.setText("Stop Server");           
        }else{
            if(!userlist.getSelectionModel().isEmpty()){
                System.out.println("was here");
                String messageToOthers = "";
                ArrayList<String> sinners =  new ArrayList(userlist.getSelectionModel().getSelectedItems());
                System.out.println("a) sinners: "+sinners.toString());
                ListIterator<String> iter = sinners.listIterator();
                while(iter.hasNext()){
                    String sinner = iter.next();
                    if(sinner.startsWith("Port: ")){
                        sinner = ":" + sinner.substring(6);
                        iter.set(sinner);
                    }else{
                        messageToOthers = messageToOthers + sinner + ", ";
                    }
                    server.banClient(sinner, "You have been banned");
                }
                userlist.getItems().removeAll(sinners);
                System.out.println("b) sinners: "+sinners.toString());
                if(sinners.size() == 1){
                    System.out.println("1 sinner");
                    server.sendToClients(new Message<>("system", Message.Type.DATA_STRING, "User "+messageToOthers+" has been banned!"));
                }else{
                    System.out.println(""+sinners.size() + " sinners");
                    server.sendToOtherClients(new Message<>("system", Message.Type.DATA_STRING, "Users "+ messageToOthers.substring(0, messageToOthers.length()-2) +" have been banned!"), sinners.toArray(new String[0]));
                }
                //userlist.setSelectionModel(null);
                if(userlist.getItems().isEmpty()){
                }

            }
        }
    }
    
    /**
     * This method is called by click on buttonSendToSelected and will send a message 
     * that is written in sendTextField to users that are currently selected
     * in ListView (nothing will be sent if there is no text in sendTextField).
     * It will not send to users in login process.
     * 
     */
    @FXML
    public void onButtonSendToSelectedClicked(){
        String inputText = sendTextField.getText().trim();
        if(inputText.equals("")) return;
        ObservableList<String> selectedUsers = userlist.getSelectionModel().getSelectedItems();
        if(inputText.startsWith("/")){
            server.serverCommand(inputText);
            sendTextField.setText("");
        }else if(!selectedUsers.isEmpty()){
            // it copies recipients from selectedUsers to ArrayList recipients, but excludes the one that start with port and shows warning
            ArrayList<String> recipients = new ArrayList<>(selectedUsers.size());
            boolean warningAlreadyShowed = false;
            for (String recipient: selectedUsers) {
                if(recipient.startsWith("Port: ")){
                    if(!warningAlreadyShowed){
                        showError("[error]: You cannot send messages to clients that are not yet logged in");
                        warningAlreadyShowed = true;
                    }
                }else{
                    recipients.add(recipient);
                }
            }
            // recipients.toArray(new String[0]) - with new String[0] you specify the type of array that will be returned by the method
            server.sendToClients(new Message<>("system", Message.Type.DATA_STRING, inputText, recipients.toArray(new String[0])));
            sendTextField.setText("");
        }
    }
    // is called by ButtonSendToAll and sendTextField
    /**
     * This method is called by clicking buttonSendToAll or clicking Enter while sendTextField
     * is focused. It will send message that is written in sendTextField to all connected users
     * (nothing will be sent if there is no text in sendTextField).
     */
    @FXML
    public void onButtonSendToAllClicked(){
        if(sendTextField.getText().matches("\\s*")) return;
        String inputText = sendTextField.getText();
        if(inputText.startsWith("/")){
            server.serverCommand(inputText);
            sendTextField.setText("");
        }else if(!userlist.getItems().isEmpty()){
            server.sendToClients(new Message<>("system", Message.Type.DATA_STRING, sendTextField.getText()));
            sendTextField.setText("");
        }
    }
    
    /**
     * This method is called when buttonPreferences is clicked. At the beginning it's
     * text will be set to "Server Preferences" and clicking it will open Setup Preferences Window.
     * After starting the Server, button's text will change to "Stop Server".
     */
    @FXML
    public void onButtonPreferences(){
        // IF BUTTON IS SET TO STOP THE SERVER ("Stop Server")
        if(buttonPreferences.getText().equals("Stop Server")){
            server.stop();
            show("[system]: Server stopped by user");
            banUser.setText("Start Server");
            banUser.setDisable(false);
            buttonPreferences.setText("Server Preferences");
            buttonSendToSelected.setText("Send To Selected User");
            serverStatus.setText("Server Stopped");
            buttonSendToSelected.setDisable(true);
            userlist.getItems().clear();
        }else{
        // IF SERVER IS STOPPED AND BUTTON IS SET TO SHOW THE PREFERENCES("Server Preferences")   
            prefsStage = new Stage();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server_gui_preferences.fxml"));
                Parent setupParent = loader.load();

                prefsController = (PreferencesController) loader.getController();          

                prefsStage.initModality(Modality.APPLICATION_MODAL); // makes the first/back window unfocusable
                prefsStage.setTitle("Preferences Setup");
                prefsStage.setScene(new Scene(setupParent));  
                prefsStage.setResizable(false);
                prefsStage.show();
                prefsController.setServerGUIController(this, autoLoad, loadFile, autoRun);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error 222");
            }
        } 
    }
    
    /**
     * This method is meant to be called by {@link PreferencesController} when Setup Preferences window
     * is open and is going to close. This method then closes the preferences window and updates {@link server_api.Server}'s
     * {@link PrivateServerPreferences} if they have changed.
     * 
     * @param prefs This are the {@link PrivateServerPreferences} that have been
     * set in Setup Preferences.
     * @param autoLoad This boolean variable defines if the Preferences file should be loaded automatically.
     * @param loadFile This is the file that will load if autoLoad is true.
     * @param autoRun This boolean variable defines if the Preferences file should start automatically (if autoLoad is true and file specified in loadFile exists).
     */
    public void closePreferencesDialog(PrivateServerPreferences prefs, boolean autoLoad, String loadFile, boolean autoRun){
        prefsStage.close();
        if(server.getPrivateServerPreferences() == null || !server.getPrivateServerPreferences().equals(prefs)){
            server.setPrivateServerPreferences(prefs);
            timeStampFormat = new SimpleDateFormat(prefs.getTimeStampFormat());
            show("[system]: Preferences updated");
        }
        this.autoLoad = autoLoad;
        this.loadFile = loadFile;
        this.autoRun = autoRun;
        banUser.setDisable(false);
    }
    
    private void showClientInfo(String username){
        if(server.isLoginRequired()){ // if login is required
            if(username.startsWith("Port: ")){ // if user is not logged in yet
                username = ":"+username.substring(6);
                System.out.println("--"+username);
                clientInfoLabel1.setText("Username: <unknown>");
                clientInfoLabel2.setText("Address: " + server.getClientAddress(username) + "     Port: " + server.getClientPort(username));
            }else{  // user is already logged in
                clientInfoLabel1.setText("Username: \""+username+"\"");
                clientInfoLabel2.setText("Address: " + server.getClientAddress(username) + "     Port: " + server.getClientPort(username));
            }
        }else{ // login is not required
        
        }
    }

    /**
     * Returns time as a String like specified in the field that can be changed through Setup Preferences window. <br>
     * Time format is the same as with SimpleDateFormat. It must look something like: dd.MM.yyyy HH:mm:ss
     * 
     * @return Returns a formatted String.
     */
    private String showTimeStamp(){
        if(timeStampFormat == null || timeStampFormat.toPattern().equals("")) return "";
        return "[" + timeStampFormat.format(new Date()) + "] ";
    }
    
    /**
     * Sets up {@link SimpleDateFormat} object like specified with argument.
     * If String equals "", it will also return true, but no time stamp will be displayed.
     * Returns true, if it was successful. It must look something like: dd.MM.yyyy HH:mm:ss
     * 
     * @param format This is the String representing how time output should look like.
     * @return Returns true, if it was successful.
     */
    protected boolean setTimeStampFormat(String format){
        if(format.equals("")) return true;
        try{
            timeStampFormat = new SimpleDateFormat(format);
            return true;
        }catch(IllegalArgumentException e){
            return false;
        }
    }
    
    /**
     * This method adds a line to TextFlow.
     * 
     * @param s This is the String that will be added.
     */
    private void show(String s){
        Text text1;
        if(timeStampFormat != null)
            text1 = new Text(showTimeStamp() + s + "\n");
        else
            text1 = new Text(s + "\n");
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textflow.getChildren().add(text1);
                scrollpane2.setVvalue(1.0);  
            }
        });
    }
    
    
    /**
     * This method adds a red line to TextFlow that represents an error.
     * 
     * @param s This is the String that will be added.
     */
    private void showError(String s){
        Text text1;
        if(timeStampFormat != null)
            text1 = new Text(showTimeStamp() + s + "\n");
        else
            text1 = new Text(s + "\n");
        text1.setFill(Color.RED);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textflow.getChildren().add(text1);
                scrollpane2.setVvalue(1.0);  
            }
        });
    }

    
}
