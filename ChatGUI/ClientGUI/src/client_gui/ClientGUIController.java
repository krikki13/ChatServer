package client_gui;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import server_api.Client;
import server_api.Message;


/**
 * The JavaFX controller class for Client control graphical interface. It is created and
 * initialized by FXMLLoader in {@link ClientGUI}. It manages most graphical things
 * and interaction with {@link server_api.Client}.
 * 
 * @author KRIKKI
 * @since 11. 7. 2017
 * @version 1.0
 */
public class ClientGUIController implements Initializable {

    @FXML
    ListView<String> userlist;
    @FXML
    TextFlow textflow;
    @FXML
    ScrollPane scrollpane1, scrollpane2;
    @FXML
    Button buttonLogoff, buttonSettings, buttonSendToAll, buttonSendToSelected;
    @FXML
    Label sendToLabel, statusLabel;
    @FXML
    TextField sendTextField;

    private String ip;
    private int port;
    private SimpleDateFormat timeStampFormat;
    private Client client;
    private Thread clientThread;
    private Preferences localPreferences;
    private boolean loggedIn = false; // so that i do not get duplicated information about being disconnected
    private String autoLoginUsername; // if program should attempt to automatically connect with specified username
    private boolean autoConnect; // if program should start connecting automatically
    private SettingsController settingsController;
    private Stage settingsStage;
    private Font font;
    private Dialog loginDialog;

    /**
     * A zero argument constructor. Not much to see here.
     */
    public ClientGUIController(){}
    
    /**
     * This method is called when stage is drawn. It creates a {@link server_api.Client}
     * object, it overrides it's println and onConnection methods and loads 
     * {@link java.util.prefs.Preferences} to obtain user settings.
     * If settings are not found it display a Dialog requesting IP and port of the server.
     * 
     * @param url I have no idea what to do with that.
     * @param rb I also have no idea what to do with that.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        localPreferences = Preferences.userNodeForPackage(this.getClass());
        ip = localPreferences.get("ip", "");
        port = localPreferences.getInt("port", -1);
        scrollpane1.setFitToWidth(true); 
        scrollpane2.setFitToWidth(true); 
        scrollpane1.setFitToHeight(true); 
        scrollpane2.setFitToHeight(true); 
        statusLabel.setText("Not connected");
        timeStampFormat = null;
        userlist.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        buttonLogoff.setText("Connect");
        
        if(ip.equals("") || port == -1){
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("IP Address And Port Setup");

            dialog.setHeaderText("Enter the IP address and the port number of the server:");
            Label label1 = new Label("IP Address:");
            Label label2 = new Label("Port: ");
            TextField ipTextField = new TextField();
            TextField portTextField = new TextField();
            Label label3 = new Label("Remember Current Setting");
            CheckBox rememberBox = new CheckBox();

            AnchorPane pane = new AnchorPane();
            pane.getChildren().addAll(label1, label2, ipTextField, portTextField, label3, rememberBox);
            AnchorPane.setLeftAnchor(label1, 10.0);
            AnchorPane.setTopAnchor(label1, 5.0);
            AnchorPane.setLeftAnchor(ipTextField, 100.0);
            AnchorPane.setTopAnchor(ipTextField, 5.0);
            AnchorPane.setRightAnchor(ipTextField, 25.0);
            AnchorPane.setLeftAnchor(label2, 10.0);
            AnchorPane.setTopAnchor(label2, 40.0);
            AnchorPane.setLeftAnchor(portTextField, 100.0);
            AnchorPane.setTopAnchor(portTextField, 40.0);
            AnchorPane.setRightAnchor(portTextField, 25.0);
            AnchorPane.setLeftAnchor(label3, 35.0);
            AnchorPane.setTopAnchor(label3, 80.0);
            AnchorPane.setLeftAnchor(rememberBox, 10.0);
            AnchorPane.setTopAnchor(rememberBox, 80.0);

            dialog.getDialogPane().setContent(pane);

            ButtonType buttonTypeOk = new ButtonType("Okay", ButtonData.OK_DONE);
            ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOk, buttonTypeCancel);

            Button okButton = (Button) dialog.getDialogPane().lookupButton(buttonTypeOk);
            okButton.addEventFilter(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("In here");
                    try {
                        String address = "";
                        String ip = ipTextField.getText().trim();
                        if (ip.equals("local")) {
                            address += "127.0.0.1";
                        } else {
                            if(!isIPAddressValid(ip)){
                                ipTextField.requestFocus();
                                ipTextField.setText("");
                                event.consume();
                                return;
                            }
                            address += ip;
                        }
                        int port = Integer.parseInt(portTextField.getText());
                        if (port < 0 || port > 65535) {
                            portTextField.requestFocus();
                            portTextField.setText("");
                            event.consume();
                            return;
                        }
                        address += "-" + port;
                        dialog.setResult(address + "--" + rememberBox.isSelected());
                    } catch (NumberFormatException e) {
                        portTextField.requestFocus();
                        portTextField.setText("");
                        event.consume();
                    }

                }
            });

            /*dialog.setOnCloseRequest((DialogEvent event) -> {
                Platform.exit();
                System.exit(0);
            });*/
            dialog.setResultConverter(new Callback<ButtonType, String>() {
                @Override
                public String call(ButtonType b) {
                    String s = dialog.getResult();
                    if (s != null) {
                        return s;
                    }
                    return "null";
                }
            });

            String res = dialog.showAndWait().get();
            if (res.equals("null")) {
                //Platform.exit();
                //System.exit(0);
                port = -1;
                ip = "";
            }else{

                ip = res.substring(0, res.indexOf("-"));
                port = Integer.parseInt(res.substring(res.indexOf("-") + 1, res.indexOf("--")));

                if(res.endsWith("--true")){ // remember checkbox was checked
                    localPreferences = Preferences.userNodeForPackage(this.getClass());
                    localPreferences.put("ip", ip);
                    localPreferences.putInt("port", port);
                }
            }
        }
        font = new Font(localPreferences.get("fontFamily", "Arial"), localPreferences.getDouble("fontSize", 12));
        autoConnect = localPreferences.getBoolean("autoConnect", false);
        if(autoConnect)
            autoLoginUsername = localPreferences.get("autoLoginUsername", "");
        try{
            timeStampFormat = new SimpleDateFormat(localPreferences.get("timeStampFormat", "HH:mm:ss"));
        }catch(IllegalArgumentException e){
            timeStampFormat = null;
        }
        
        
        client = new Client() {
            @Override
            public void println(String s) {
                show(s);
            }

            @Override
            public void errPrintln(String s) {
                showError(s);
            }

            @Override
            public void onLoginRequired() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if(autoConnect && !autoLoginUsername.equals("")){
                            login(autoLoginUsername);  
                            show("[system]: Attempted automatic login with username " + autoLoginUsername);
                        }else{
                            loginDialog = new TextInputDialog("");
                            loginDialog.setTitle("Login");
                            loginDialog.setHeaderText("Enter your username:");
                            Optional<String> result = loginDialog.showAndWait();
                            if (result.isPresent()) {
                                login(result.get());
                            }else{
                                if(loggedIn){ // if logged in is false it means that connection stopped when the window was opened
                                    show("[system] Login is mandatory for connection with this server");
                                    close();
                                }
                            }
                        }
                    }
                });
            }
            
            @Override
            public void onLoginSuccessful(String message){
                super.onLoginSuccessful(message);
                Platform.runLater(() -> {
                    statusLabel.setText(client.getUsername() + " ("+client.getPort()+")");
                    buttonLogoff.setText("Logoff");
                });
                sendMessage(new Message<>(client.getUsername(), Message.Type.COMMAND, "/who -c"));
                
            }
            
            @Override
            public void onLoginDenied(String reason){
                if(reason.equals("Username already exists. Pick another one")){
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Dialog dialog = new TextInputDialog("");
                            dialog.setTitle("Login");
                            dialog.setHeaderText(reason + "\nEnter your username:");
                            Optional<String> result = dialog.showAndWait();
                            if (result.isPresent()) {
                                login(result.get());
                            }else{
                                show("[system]: Login is mandatory for connection with this server");
                                close();
                            }
                        }
                    });
                }else{
                    showError("[error]: " + reason);
                    
                }
            }

            @Override
            public void onConnectionClosed(String reason) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        if(loggedIn && !reason.equals(""))
                            showError("[error]: "+reason);
                        loggedIn = false;
                        if(loginDialog != null && loginDialog.isShowing())
                            loginDialog.close();
                        statusLabel.setText("Not Connected");
                        userlist.getItems().clear();
                        buttonLogoff.setText("Connect");
                    }
                });
            }

            @Override
            public void onNewUserConnected(String username) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        show("[system]: User " + username + " has connected");
                        userlist.getItems().add(username);
                    }
                });
            }

            @Override
            public void onUserDisconnected(String username) {
                System.out.println("onUserDisconnected("+ username +")");
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        // to se zdej izpisuje 2x
                        show("[system]: User " + username + " has disconnected");
                        userlist.getItems().remove(username);
                    }
                });
            }
            
            @Override
            public void onSystemMessageReceived(String description, String value){
                System.out.printf("desc: %s, value: %s\n", description, value);
                switch(description){
                    case "recipients-not-exist":
                        errPrintln("[system]: Following recipients do not exist: " + value);
                        break;
                    case "connected-clients":
                        Platform.runLater(() -> {
                            userlist.getItems().clear();
                            for(String user: value.split(",")){
                                if(!user.equals(client.getUsername()))
                                    userlist.getItems().add(user);
                            }
                        });
                        break;
                    default:
                        errPrintln("[error]: System message not recognized");
                }
            }

        };
        
        if(port == -1 && ip.equals("")){
            show("[system]: You need to set server IP address and port before connecting");
        }else if(ip.equals("")){
            show("[system]: You need to set server IP address before connecting");
        }else if(port == -1){
            show("[system]: You need to set server port before connecting");
        }else{
            show("[system]: Server IP has been set to "+ip+" and port to "+port);
            client.setAddress(ip);
            client.setPort(port);
            if(autoConnect){
                loggedIn = true;
                clientThread = new Thread(client);
                clientThread.start();
                statusLabel.setText("Connecting (at port "+port+")");
                buttonLogoff.setText("Cancel");
            }
        }
    }
    
    /**
     * This method is called by clicking buttonSendToAll or clicking Enter while sendTextField
     * is focused. It will send message that is written in sendTextField to all connected users
     * (nothing will be sent if there is no text in sendTextField).
     */
    @FXML
    public void onButtonSendToAllClicked() {
        String toSend = sendTextField.getText();
        if(!toSend.equals("")){
            if(toSend.matches(client.getPublicServerPreferences().getForbiddenWords())){
                showError("[error]: A word you tried to send was inappropriate");
            }else{
                client.sendText(toSend);
            }
        }
        sendTextField.setText("");
    }

    /**
     * This method is called by click on buttonSendToSelected and will send a message 
     * that is written in sendTextField to users that are currently selected
     * in ListView (nothing will be sent if there is no text in sendTextField).
     * It will also send back to this user (you).
     */
    @FXML
    public void onButtonSendToSelectedClicked() {
        String inputText = sendTextField.getText().trim();
        if(inputText.equals("")) return;
        ObservableList<String> selectedUsers = userlist.getSelectionModel().getSelectedItems();
        if(inputText.startsWith("/")){
            client.sendCommand(inputText);
            sendTextField.setText("");
        }else if(!selectedUsers.isEmpty() && this.loggedIn){
            // recipients.toArray(new String[0]) - with new String[0] you specify the type of array that will be returned by the method
            String[] recipients = new String[selectedUsers.size()+1];
            for (int i = 0; i < recipients.length-1; i++) {
                recipients[i] = selectedUsers.get(i);
            }
            recipients[recipients.length-1] = this.client.getUsername();
            client.sendText(inputText, recipients);
            sendTextField.setText("");
        }
    }

    /**
     * Returns server IP which Client for connection with Server.
     * 
     * @return Returns server IP.
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns server port which Client for connection with Server.
     * 
     * @return Returns server port.
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Returns {@link javafx.scene.text.Font} in which text will be displayed on the TextFlow.
     * 
     * @return Returns the {@link javafx.scene.text.Font} in which text will be displayed.
     */
    public Font getFont() {
        return font;
    }
    
    /**
     * Returns true if Client should attempt to connect automatically.
     * 
     * @return Returns true if Client should connect automatically.
     */
    public boolean getAutoConnect() {
        return autoConnect;
    }
    
    /**
     * Returns the String Client uses to login automatically. If ""
     * it will not connect automatically.
     * 
     * @return Returns the String Client uses to login automatically.
     */
    public String getAutoLoginUsername() {
        return autoLoginUsername;
    }
    
    /**
     * Returns the format for time stamps that are written in TextFlow.
     * It uses the same format as {@link java.text.SimpleDateFormat}.
     * If time stamps are not shown, it will return "".
     * 
     * @return Returns the format for time stamps.
     */
    public String getTimeStampFormat() {
        if(timeStampFormat == null) return "";
        return timeStampFormat.toPattern();
    }
    
    /**
     * This method is called when the Logoff button is clicked.
     * If Client is not connected at the moment (button's text is "Connect"),
     * it will start connecting, if client is already connected it will logoff by
     * sending logoff message to Server. If Client is trying to connect (button's text 
     * is "Cancel") you can click to cancel connection. 
     * 
     */
    @FXML
    public void onLogoffClicked() {
        if(buttonLogoff.getText().equals("Connect")){
            buttonLogoff.setText("Cancel");
            client.setPort(port);
            client.setAddress(ip);
            clientThread = new Thread(client);
            clientThread.start();
            loggedIn = true;
            statusLabel.setText("Connecting (at port "+port+")");
        }else{ // Logoff
            client.logoff();
            if(buttonLogoff.getText().equals("Cancel")){
                loggedIn = false;
                show("[system]: Connection canceled");
            }else{
                show("[system]: Logoff successful");
                statusLabel.setText("Not Connected");
                buttonLogoff.setText("Connect");
                loggedIn = false;
            }
        }
    }

    /**
     * This method is called when the Settings button is clicked.
     * It opens Settings window and gives control to {@link SettingsController}.
     */
    @FXML
    public void onButtonSettingsClicked() {
        settingsStage = new Stage();
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/client_gui_settings.fxml"));
                Parent setupParent = loader.load();

                settingsController = (SettingsController) loader.getController();          

                settingsStage.initModality(Modality.APPLICATION_MODAL); // makes the first/back window unfocusable
                settingsStage.setTitle("Settings");
                settingsStage.setScene(new Scene(setupParent));  
                settingsStage.setResizable(false);
                settingsStage.show();
                settingsController.setServerGUIController(this);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error 222");
            }
    }
    
    /**
     * This method is meant to be called by {@link SettingsController}, when it needs
     * to close. It receives all the data Settings window can obtain through arguments.
     * If values of some arguments are invalid (like IP or autoLoginUsername), there
     * will be an error at runtime. New values of IP, port and autoLoginUsername, do
     * not affect current connection, but yet the next one.
     * 
     * @param ip This is the new IP of the Server.
     * @param port This is the new port of the Server.
     * @param autoConnect This defines if {@link ClientGUIController} should attempt connecting automatically.
     * @param autoLoginUsername This is the new username, if user wants login to be automatic.
     * @param font This is the new Font that will start affecting immediately, but have no effect on already written text.
     * @param timeStampFormat This is the format of time stamps that appear in front of text. It can be null.
     */
    public void closeSettingsWindow(String ip, int port, boolean autoConnect, String autoLoginUsername, Font font, SimpleDateFormat timeStampFormat){
        this.ip = ip;
        this.port = port;
        this.autoConnect = autoConnect;
        this.autoLoginUsername = autoLoginUsername;
        this.font = font;
        this.timeStampFormat = timeStampFormat;
        settingsStage.close();
    }

    /**
     * Returns time as a String like specified in the field that can be changed
     * through Setup Preferences window. <br>
     * Time format is the same as with SimpleDateFormat. It must look something
     * like: dd.MM.yyyy HH:mm:ss
     *
     * @return Returns a formatted String.
     */
    private String showTimeStamp() {
        if (timeStampFormat == null) {
            return "";
        }
        return "[" + timeStampFormat.format(new Date()) + "] ";
    }

    /**
     * Sets up {@link SimpleDateFormat} object like specified with argument. If
     * String equals "", it will also return true, but no time stamp will be
     * displayed. Returns true, if it was successful. It must look something
     * like: dd.MM.yyyy HH:mm:ss
     *
     * @param format This is the String representing how time output should look
     * like.
     * @return Returns true, if it was successful.
     */
    protected boolean setTimeStampFormat(String format) {
        if (format.equals("")) {
            return true;
        }
        try {
            timeStampFormat = new SimpleDateFormat(format);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * This method adds a line to TextFlow.
     *
     * @param s This is the String that will be added.
     */
    private void show(String s) {
        Text text1;
        if (timeStampFormat != null) {
            text1 = new Text(showTimeStamp() + s + "\n");
        } else {
            text1 = new Text(s + "\n");
        }
        text1.setFont(font);
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
    private void showError(String s) {
        Text text1;
        if (timeStampFormat != null) {
            text1 = new Text(showTimeStamp() + s + "\n");
        } else {
            text1 = new Text(s + "\n");
        }
        text1.setFill(Color.RED);
        text1.setFont(font);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                textflow.getChildren().add(text1);
                scrollpane2.setVvalue(1.0);  
            }
        });
    }
    
    /**
     * Makes a quick check if IP address (IPv4 or IPv6) is valid. This does not ensure 
     * the validity, but makes a decent approach.
     * 
     * @param ip This is the IP address.
     * @return Returns true if IP address seems to be valid.
     */
    public static boolean isIPAddressValid(String ip){
        //if (!ip.matches("[012]?\\d?\\d\\.[012]?\\d?\\d\\.[012]?\\d?\\d\\.[012]?\\d?\\d")
         //   && (ip.matches("^$|.*[^a-fA-F0-9:].*|.*[a-fA-F0-9]{5,}.*|.*::.*::.*") || ip.length() - ip.replace(":", "").length() > 7)) { false 
        return ip.matches("[012]?\\d?\\d\\.[012]?\\d?\\d\\.[012]?\\d?\\d\\.[012]?\\d?\\d") || !(ip.matches("^$|.*[^a-fA-F0-9:].*|.*[a-fA-F0-9]{5,}.*|.*::.*::.*") || ip.length() - ip.replace(":", "").length() > 7);
    }
}
