package client_gui;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;

/**
 * This is JavaFX Controller for Client Settings window that is created and initialized
 * by FXMLLoader in {@link ClientGUIController}. It shows a window where you can easily set
 * port and IP address for connecting with Server and some personal prefereneces. 
 * Values are saved with {@link java.util.prefs.Preferences} 
 * and can also be seen with Registry Editor (HKEY_CURRENT_USER/SOFTWARE/JavaSoft/Prefs/client_gui).
 * 
 * @author KRIKKI
 * @since 13.7.2017
 * @version 1.0
 */
public class SettingsController implements Initializable {

    @FXML
    TextField portTextField, ipTextField, autoLoginTextField;
    @FXML
    TextField fontSizeTextField, fontTestField, timeStampTextField;
    @FXML
    ComboBox<String> fontFamilyComboBox;
    @FXML
    Button doneButton;
    @FXML
    CheckBox autoConnectBox;
    
    private ClientGUIController clientGUIController;
    private Preferences localPreferences;
    Font testFont;
    
    /**
     * A zero argument constructor. Not much to see here.
     */
    public SettingsController(){}
    
    /**
     * This method is called when settings stage is drawn. 
     * 
     * @param url I have no idea what to do with that.
     * @param rb I also have no idea what to do with that.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        localPreferences = Preferences.userNodeForPackage(this.getClass());
        
        fontFamilyComboBox.getItems().addAll(Font.getFamilies());
        
        //autoLoad = localPreferences.getBoolean("autoLoad", false);
        
        // on lost focus on fontSizeTextField
        fontSizeTextField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue){
                if (!newPropertyValue)
                    updateFontTest();
            }
        });
       
    }
    
    /**
     * This method is called by parent class {@link ClientGUIController} immediately
     * after initialisation. SettingsController now gets reference to it's parent.
     * 
     * @param clientGUIController This is the reference to parent class.
     */
    void setServerGUIController(ClientGUIController clientGUIController){
        this.clientGUIController = clientGUIController;
        ipTextField.setText(clientGUIController.getIp());
        int port1 = clientGUIController.getPort();
        if(port1 > 0)
            portTextField.setText("" + port1);
        
        testFont = clientGUIController.getFont();
        fontSizeTextField.setText("" + clientGUIController.getFont().getSize());
        fontFamilyComboBox.getSelectionModel().select(clientGUIController.getFont().getFamily());
        autoLoginTextField.setText(clientGUIController.getAutoLoginUsername());
        timeStampTextField.setText(clientGUIController.getTimeStampFormat());
        autoConnectBox.setSelected(clientGUIController.getAutoConnect());
        
        if(clientGUIController.getAutoConnect())
            autoLoginTextField.setDisable(false);
        else
            autoLoginTextField.setDisable(true);

    }

    /**
     * This method is called when the Done button is clicked. It checks if values
     * of variables in TextFields seem valid and if so it calls {@link ClientGUIController#closeSettingsWindow(java.lang.String, int, boolean, java.lang.String, javafx.scene.text.Font, java.text.SimpleDateFormat)},
     * which then closes this Window. If values are not valid, it shows an error
     * Dialog describing the problem.
     */
    @FXML
    public void onDoneButtonClicked(){
        // check port
        int port1;
        try{
            port1 = Integer.parseInt(portTextField.getText().trim());
            if(port1 < 1024 || port1 > 65535){
                portTextField.setText("");
                portTextField.requestFocus();
                return;
            }
        }catch(NumberFormatException e){
            portTextField.setText("");
            portTextField.requestFocus();
            return;
        }
        // check ip
        String ip1 = ipTextField.getText().trim();
        if(!ClientGUIController.isIPAddressValid(ip1)){
            ipTextField.requestFocus();
            return;
        }

        // check font
        try{
            double size  = Double.parseDouble(fontSizeTextField.getText().replace(",", ".").trim());
            if(size > 100){
                fontSizeTextField.setText("100.0");
                fontSizeTextField.requestFocus();
                return;
            }else if(size < 1){
                fontSizeTextField.setText("1.0");
                fontSizeTextField.requestFocus();
                return;
            }
            testFont = new Font(fontFamilyComboBox.getSelectionModel().getSelectedItem(), size);
            fontTestField.setFont(testFont);
        }catch(NumberFormatException e){
            fontSizeTextField.setText("");
            fontSizeTextField.requestFocus();
            return;
        }
        // check timeStampFormat
        SimpleDateFormat sdf = null;
        String timeFormat = timeStampTextField.getText().trim();
        if(!timeFormat.equals("")){
            try{
                sdf = new SimpleDateFormat(timeStampTextField.getText());
            }catch(IllegalArgumentException e){
                timeStampTextField.setText("");
                timeStampTextField.requestFocus();
                return;
            }
        }
        boolean autoConnect = autoConnectBox.isSelected();
        String autoLogin = "";
        
        localPreferences.put("ip", ip1);
        localPreferences.putInt("port", port1);
        localPreferences.putBoolean("autoConnect", autoConnect);
        if(autoConnect){
            autoLogin = autoLoginTextField.getText().trim();
            localPreferences.put("autoLoginUsername", autoLogin);
        }
        localPreferences.put("fontFamily", testFont.getFamily());
        localPreferences.putDouble("fontSize", testFont.getSize());
        if(sdf != null)
            localPreferences.put("timeStampFormat", sdf.toPattern());
        clientGUIController.closeSettingsWindow(ip1, port1, autoConnect, autoLogin, testFont, sdf);    
    }
    
    /**
     * This method is called by pressing Enter when fontSizeTextField is focused or when
     * fontSizeTextField loses focus or when action occurs on fontFamilyComboBox.
     * It then changes font of fontTestField, so user can have a preview of new Font.
     */
    public void updateFontTest(){
        try{
            double size  = Double.parseDouble(fontSizeTextField.getText().replace(",", "."));
            if(size > 100){
                fontSizeTextField.setText("100.0");
                fontSizeTextField.requestFocus();
            }else if(size < 1){
                fontSizeTextField.setText("1.0");
                fontSizeTextField.requestFocus();
            }
            testFont = new Font(fontFamilyComboBox.getSelectionModel().getSelectedItem(), Double.parseDouble(fontSizeTextField.getText()));
            fontTestField.setFont(testFont);
        }catch(NumberFormatException e){
            fontSizeTextField.setText("");
            fontSizeTextField.requestFocus();
        }
    }
    
    /**
     * This method is called when autoConnectBox changes its state.
     * It then disable or enables the autoLoginTextField.
     */
    @FXML
    public void autoConnectBoxChanged(){
        if(autoConnectBox.isSelected())
            autoLoginTextField.setDisable(false);
        else
            autoLoginTextField.setDisable(true);
    }
    
    /**
     * This method shows an error dialog.
     * 
     * @param title This is the title of the dialog.
     * @param content This is the content of the dialog.
     */
    private void showErrorDialog(String title, String content){
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();

    }
    

    
}