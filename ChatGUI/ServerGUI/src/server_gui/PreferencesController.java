package server_gui;

import java.awt.dnd.Autoscroll;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import server_api.PrivateServerPreferences;
import server_api.ServerException;

/**
 * This is JavaFX Controller for Setup Preferences window that is created and initialized
 * by FXMLLoader in {@link ServerGUIController}. It shows a window where you can easily set
 * {@link server_api.PrivateServerPreferences}. It also allows you to load a Preferences from 
 * a file and if desired automatically load and run them. The latter 2 boolean values and 
 * path to Preferences file are saved with {@link java.util.prefs.Preferences} 
 * and can also be seen with Registry Editor (HKEY_CURRENT_USER/SOFTWARE/JavaSoft/Prefs/server_gui).
 * 
 * 
 * @author KRIKKI
 * @since 6.7.2017
 * @version 1.0
 */
public class PreferencesController implements Initializable {

    @FXML
    TextField loadFromTextField;
    @FXML
    Button buttonBrowse, buttonLoad, buttonUpdate, buttonCreateTextFile, buttonCreateBinaryFile;
    @FXML
    CheckBox autoLoadCheckBox, autoRunCheckBox;
    @FXML
    TextField portTextField;
    @FXML
    CheckBox loginRequiredCheckBox;
    @FXML
    TextField usernameLengthFrom, usernameLengthTo;
    @FXML
    TextField maxNumTextField;
    @FXML
    TextField allowedUsernamesTextField, forbiddenUsernamesTextField, forbiddenWordsTextField;
    @FXML
    TextField regexStringTest, regexInputTest;
    @FXML
    Button buttonRegexTest;
    @FXML
    Label regexTestResult;
    @FXML
    Button buttonDone;
    @FXML
    TextField timeStampFormatTextField;
    @FXML
    Label updateWarningLabel;
    
    private PrivateServerPreferences serverPrefs;
    private Preferences localPreferences;
    ServerGUIController serverGUIController;
    ToastNotification toast;
    
    /**
     * A zero argument constructor. Not much to see here.
     */
    public PreferencesController(){}

    /**
     * This method is called when preferences stage is drawn. If autoLoad
     * was set to true, it prepares to load a file. It will be actually
     * loaded in method {@link #setServerGUIController(server_gui.ServerGUIController, boolean, java.lang.String, boolean)}
     * when the reference to parent class is obtained.
     * 
     * @param url I have no idea what to do with that.
     * @param rb I also have no idea what to do with that.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        localPreferences = Preferences.userNodeForPackage(this.getClass());
        /*
        autoLoad = localPreferences.getBoolean("autoLoad", false);
        if(autoLoad){
            autoRun = localPreferences.getBoolean("autoRun", false);

            loadedFile = localPreferences.get("loadedFile", "");
            System.out.println("loadedFile: "+loadedFile);
            if (loadedFile.equals("") || !new File(loadedFile).exists()) {
                autoLoad = false;
                autoLoadCheckBox.setSelected(false);
                autoRun = false;
                autoRunCheckBox.setSelected(false);

            }else{
                autoLoadCheckBox.setSelected(autoLoad);
                autoRunCheckBox.setSelected(autoRun);
                loadFromTextField.setText(loadedFile);  
            }
        }
        */
        autoLoadCheckBox.addEventHandler(EventType.ROOT, new EventHandler() {
            @Override
            public void handle(Event event) {
                if(autoLoadCheckBox.isSelected()){
                    autoRunCheckBox.setDisable(false);
                }else{
                    autoRunCheckBox.setDisable(true);
                }
            }
        });
        loginRequiredCheckBox.addEventHandler(EventType.ROOT, new EventHandler() {
            @Override
            public void handle(Event event) {
                if(loginRequiredCheckBox.isSelected()){
                    usernameLengthFrom.setDisable(false);
                    usernameLengthTo.setDisable(false);
                    allowedUsernamesTextField.setDisable(false);
                    forbiddenUsernamesTextField.setDisable(false);
                           
                }else{
                    usernameLengthFrom.setDisable(true);
                    usernameLengthTo.setDisable(true);
                    allowedUsernamesTextField.setDisable(true);
                    forbiddenUsernamesTextField.setDisable(true);
                }
            }
        });
    }
    
    /**
     * This method is called by parent class {@link ServerGUIController} immediately
     * after initialization. PreferencesController now gets reference to it's parent
     * and is able to get {@link server_api.PrivateServerPreferences} from parent if 
     * they have been set before.
     * 
     * @param serverGUIController This is the reference to parent class.
     */
    void setServerGUIController(ServerGUIController serverGUIController, boolean autoLoad, String loadFile, boolean autoRun){
        this.serverGUIController = serverGUIController;

        autoLoadCheckBox.setSelected(autoLoad);
        loadFromTextField.setText(loadFile);
        if(autoLoad)
            autoRunCheckBox.setSelected(autoRun);
        else{
            autoRunCheckBox.setSelected(false);
            autoRunCheckBox.setDisable(true);
        }
        serverPrefs = new PrivateServerPreferences(serverGUIController.server.getPrivateServerPreferences());
        if(serverPrefs != null && serverPrefs.isValid())
            loadFromPrefsObject();
 
        toast = new ToastNotification(serverGUIController.prefsStage, 3000, 500, 500);
    }

    /**
     * This method is called when buttonBrowse is clicked and it opens a FileChooser
     * dialog in which user can select Preferences file and it's path is written
     * to TextField.
     */
    @FXML
    public void onButtonBrowseClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Preferences File");
        // sets the initial dir to the same one as it was befoe
        File currentFile = new File(loadFromTextField.getText());
        fileChooser.setInitialDirectory(currentFile.getParentFile());
        if(currentFile.exists()){
        
        }
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            loadFromTextField.setText(selectedFile.getAbsolutePath());
        }
    }
    /**
     * This method is called when buttonCreateTextFile is clicked and it 
     * creates an empty new text file (with help of {@link #createFile(java.lang.String, java.lang.String)}).
     * 
     */
    @FXML
    public void onButtonCreateTextFile() {
        createFile("Prefs Text(*.tsp)", "*.tsp");
        
    }
    /**
     * This method is called when buttonCreateTextFile is clicked and it 
     * creates an empty new binary file (with help of {@link #createFile(java.lang.String, java.lang.String)}).
     * 
     */
     @FXML
    public void onButtonCreateBinaryFile() {
        createFile("Prefs Binary(*.bsp)", "*.bsp");
    }

    /**
     * This method is called by {@link #onButtonCreateTextFile() } and {@link #onButtonCreateBinarytFile()}
     * and creates text or binary file.<br>
     * An example for typeDescription is "Prefs Binary(*.bsp)", 
     * an example for typeEnd is "*.bsp"
     * 
     * @param typeDescription This is description of the type for FileChooser.
     * @param typeEnd This defines the allowed file extension.
     */
    private void createFile(String typeDescription, String typeEnd) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Preferences File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(typeDescription, typeEnd));
        File selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile == null) {
            return;
        }
        loadFromTextField.setText(selectedFile.getAbsolutePath());

        if(selectedFile.exists()){
            showErrorDialog("File already exists", "The file you specified already exists.\nYou need to manually delete it before you can create a new Preferences file.");
            return;
        }
        try {
            FileWriter fw = new FileWriter(selectedFile);
            fw.write("");
            fw.close();
            if(typeEnd.equals("*.bsp"))
                toast.show("Created binary file "+selectedFile.getName());
            else if(typeEnd.equals("*.tsp"))
                toast.show("Created text file "+selectedFile.getName());
        } catch (IOException ex) {
            System.out.println("Error 521");
        }
    }

    /**
     * This method is called when buttonLoad is clicked and it attempts to read Preferences
     * from the file specified in the TextField. It is important that the file extension is
     * either .tsp or .bsp and nothing else. If successful it then writes preferences to 
     * TextFields below (this is done using method {@link #loadFromPrefsObject()}).
     */
    @FXML
    public void onButtonLoadClicked() {
        String filename = loadFromTextField.getText();
        if(!new File(filename).exists()) {
            showErrorDialog("File Does Not Exist", "The file which you have specified does not exist!");
            return;
        }
        
        if (filename.endsWith(".bsp")) {
            try {
                serverPrefs = PrivateServerPreferences.readFromBinaryFile(filename);
                toast.show("Loaded binary file");
            } catch (IOException ex) {
                System.out.println("IO Exception");
                return;
            } catch (ServerException ex) {
                showErrorDialog("Incorrect file format", "The file is not correctly written!");
                return;
            }
        } else if (filename.endsWith(".tsp")) {
            try {
                serverPrefs = PrivateServerPreferences.readFromTextFile(filename);
                toast.show("Loaded text file");
            } catch (IOException ex) {
                System.out.println("IO Exception");
                return;
            } catch (ServerException ex) {
                showErrorDialog("Variable not recognized", ex.getMessage());
                return;
            }
        } else {
            showErrorDialog("Wrong File Extension", "File extension for preferences files must be .bsp (binary) or .tsp (text)!");
            return;
        }
        loadFromPrefsObject();
    }
    
    /**
     * This method will read preferences from global PrivateServerPreferences object
     * and will write them to TextFields where they can be edited.
     */
    private void loadFromPrefsObject(){
        portTextField.setText(""+serverPrefs.getPort());
        loginRequiredCheckBox.setSelected(serverPrefs.isLoginRequired());
        usernameLengthFrom.setText(""+serverPrefs.getMinUsernameLength());
        usernameLengthTo.setText(""+serverPrefs.getMaxUsernameLength());
        maxNumTextField.setText(""+serverPrefs.getMaxNumberOfClients());
        allowedUsernamesTextField.setText(serverPrefs.getAllowedUsernames());
        forbiddenUsernamesTextField.setText(serverPrefs.getForbiddenUsernames());
        forbiddenWordsTextField.setText(serverPrefs.getForbiddenWords());
        timeStampFormatTextField.setText(serverPrefs.getTimeStampFormat());
    }
    
    /**
     * This method is called when buttonUpdate is clicked and it reads Preferences from
     * TextFields and attempts to save them file written in File TextField.
     * Reading is done using method {@link #loadToPrefsObject()}.
     */
    @FXML
    public void onButtonUpdateClicked() {
        String filename = loadFromTextField.getText();
        File file = new File(filename);
        if(!file.exists()) {
            showErrorDialog("File Does Not Exist", "The file which you have specified does not exist!");
            return;
        }
        loadToPrefsObject();
        if (filename.endsWith(".bsp")) {
            try {
                PrivateServerPreferences.writeToBinaryFile(serverPrefs, filename);
                toast.show("Updated binary file "+ file.getName());
                
            } catch (Exception ex) {
                String msg = ex.getMessage();
                if(msg.endsWith("(The process cannot access the file because it is being used by another process)"))
                    msg = "The process cannot access the file because it is being used by another process!";
                showErrorDialog("Error", msg);
            }
        } else if (filename.endsWith(".tsp")) {
            try {
                PrivateServerPreferences.writeToTextFile(serverPrefs, filename);
                toast.show("Updated text file "+ file.getName());
            } catch (IOException ex) {
                String msg = ex.getMessage();
                if(msg.endsWith("(The process cannot access the file because it is being used by another process)"))
                    msg = "The process cannot access the file because it is being used by another process!";
                showErrorDialog("Error", msg);
            }
        } else {
            showErrorDialog("Wrong File Extension", "File extension for preferences files must be .bsp (binary) or .tsp (text)!");
        }
    }
    
    /**
     * This method reads from TextFields and saves to {@link server_api.PrivateServerPreferences}.
     * If text in a TextField is not right type it will focus that TextField, delete
     * incorrect text and retun false.
     * 
     * @return Returns true if it was successful.
     */
    private boolean loadToPrefsObject(){
        // if port input is ok
        try{
            int newPort = Integer.parseInt(portTextField.getText());
            if(newPort < 1024 || newPort > 65535){
                showErrorDialog("Incorrect Value", "Port number must be an integer between 1024 and 65535");
                return false;
            }
            serverPrefs.setPort(Integer.parseInt(portTextField.getText()));
        }catch(NumberFormatException ex){
            showErrorDialog("Incorrect Value", "Port number must be an integer between 1024 and 65535");
            return false;    
        }

        serverPrefs.setLoginRequired(loginRequiredCheckBox.isSelected());
        // if usernameLengthFrom is ok
        int usernameMinLength;
        try{
            usernameMinLength = Integer.parseInt(usernameLengthFrom.getText());
            if(usernameMinLength < 1){
                showErrorDialog("Incorrect Value", "Minimal username length must be an integer greater or equal to 1");
                return false;
            }
            serverPrefs.setMinUsernameLength(usernameMinLength);
        }catch(NumberFormatException ex){
            showErrorDialog("Incorrect Value", "Mimimal username length must be an integer greater or equal to 1");
            return false;    
        }
        try{
            int usernameMaxLength = Integer.parseInt(usernameLengthTo.getText());
            if(usernameMaxLength < usernameMinLength){
                showErrorDialog("Incorrect Value", "Maximal username length must be an integer greater or equal to minimal username length");
                return false;
            }
            serverPrefs.setMaxUsernameLength(usernameMaxLength);
        }catch(NumberFormatException ex){
            showErrorDialog("Incorrect Value", "Maximal username length must be an integer greater or equal to minimal username length");
            return false;    
        }
        try{
            int maxClients = Integer.parseInt(maxNumTextField.getText());
            if(maxClients < 1){
                showErrorDialog("Incorrect Value", "Maximal number of clients must be an integer greater or equal to 1");
                return false;
            }
            serverPrefs.setMaxNumberOfClients(maxClients);
        }catch(NumberFormatException ex){
            showErrorDialog("Incorrect Value", "Maximal number of clients must be an integer greater or equal to 1");
            return false;    
        }
        
        serverPrefs.setAllowedUsernames(allowedUsernamesTextField.getText());
        serverPrefs.setForbiddenUsernames(forbiddenUsernamesTextField.getText());
        serverPrefs.setForbiddenWords(forbiddenWordsTextField.getText());
        
        try{
            String newFormat = timeStampFormatTextField.getText().trim();
            new SimpleDateFormat(newFormat);
            serverPrefs.setTimeStampFormat(newFormat);
        }catch(IllegalArgumentException ex){
            showErrorDialog("Incorrect Value", "Incorrect format for time stamp. It should look like dd.MM.yyyy HH:mm:ss", "dd - day of month\nMM - month\nyyyy - year\nHH - hour (24 hours)\nhh - (12 hours)\nmm - minutes\nss - seconds\naa - AM or PM");
            return false;    
        }
        
        if(!serverPrefs.isValid()){
            System.out.println("Unknown error 712");
            System.exit(1);
        }
        return true;  
    }
    
    /**
     * This method is called when buttonDone is clicked and it reads Preferences from
     * TextFields using method {@link #loadToPrefsObject()}. If it was successful, it saves autoLoad,
     * autoRun and loadedFile variables to {@link java.util.prefs.Preferences} if file
     * exists and finally calls {@link ServerGUIController#closePreferencesDialog(server_api.PrivateServerPreferences, boolean, java.lang.String, boolean)}
     * to close Setup Preferences window.
     * 
     */
    @FXML
    public void onButtonDoneClicked(){
        // reads preferences from TextFields and saves to PrivateServerPreferences
        if(!loadToPrefsObject()) return;
        // read time format from TextField and saves to field
        
        String filename = loadFromTextField.getText();
        if(new File(filename).exists() &&  (filename.endsWith(".tsp") || filename.endsWith(".bsp"))){
            localPreferences.put("loadFile", filename);
            localPreferences.putBoolean("autoLoad", autoLoadCheckBox.isSelected());
            localPreferences.putBoolean("autoRun", autoRunCheckBox.isSelected());
        }
        serverGUIController.closePreferencesDialog(serverPrefs, autoLoadCheckBox.isSelected(), loadFromTextField.getText().trim(), autoRunCheckBox.isSelected());
    }
    
    /**
     * This method shows an error dialog.
     * 
     * @param title This is the title of the dialog.
     * @param content This is the content of the dialog.
     * @see #showErrorDialog(java.lang.String, java.lang.String) 
     */
    private void showErrorDialog(String title, String header, String content){
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();

    }
    
    /**
     * This method shows an error dialog, with text only in header.
     * 
     * @param title This is the title of the dialog.
     * @param content This is the content of the dialog.
     * @see #showErrorDialog(java.lang.String, java.lang.String, java.lang.String) 
     */
    private void showErrorDialog(String title, String header){
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.show();

    }
    
    /**
     * This method will test if Regex input in TextField matches Regex String
     * written in another TextField. It is only for testing purposes and it does not get
     * saved anywhere.
     * 
     */
    @FXML
    public void onButtonRegexTestClicked(){
        if(regexInputTest.getText().matches(regexStringTest.getText())){
            regexTestResult.setText("Matches");
        }else{
            regexTestResult.setText("Does Not Match");
        }
    }
    
    /**
     * This method is called when mouse enters the area of the Done button.
     * It then changes the warning message to bold style, so that user does 
     * not miss it.
     * 
     * @see #onButtonDoneMouseEntered() 
     */
    @FXML
    public void onButtonDoneMouseEntered(){
        updateWarningLabel.setStyle("-fx-font: bold 12px system");
    }
    
    /**
     * This method is called when mouse leaves the area of the Done button.
     * It then changes the warning message back to regular style.
     * 
     * @see #onButtonDoneMouseExited() 
     */
    @FXML
    public void onButtonDoneMouseExited(){
        updateWarningLabel.setStyle("-fx-font: 13px system");
    }
}