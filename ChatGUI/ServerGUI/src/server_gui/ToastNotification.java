/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server_gui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * This class is used for showing undecorated information dialogs that disappear automatically
 * after some time with a nice fading animation. It is very similar Android's Toasts. It's static method 
 * {@link #showText(javafx.stage.Stage, java.lang.String, int, int, int)}
 * can be used to quickly show a Toast, however if many toast will be showed it is 
 * better to create ToastNotification object and then showing it from {@link #show(java.lang.String)}.
 * 
 * @author KRIKKI
 * @version 1.0
 * @since 6.7.2017
 */
public class ToastNotification {
    private Stage toastStage;
    private int toastTime;
    private int fadeInDelay;
    private int fadeOutDelay;
    private Font font;
    private Color textColor;
    private Text text;
    private Timeline fadeInTimeline;
    
    /**
     * Creates a simple Toast object that will define how Toast will look.
     * If you wish to set more details use the other constructor.
     * Toast can be then shown with {@link #show(java.lang.String)}.
     * 
     * @param parentStage This is the stage of the parent class.
     * @param toastTime This defines how long will toast be shown with full opacity.
     * @param fadeInDelay This defines how long the fade in animation will last.
     * @param fadeOutDelay This defines how long the fade out animation will last.
     * @see #ToastNotification(javafx.stage.Stage, int, int, int, javafx.scene.text.Font, javafx.scene.paint.Color) 
     */
    public ToastNotification(Stage parentStage, int toastTime, int fadeInDelay, int fadeOutDelay) {
        this.toastTime = toastTime;
        this.fadeInDelay = fadeInDelay;
        this.fadeOutDelay = fadeOutDelay;
        this.font = Font.font("Verdana", 18);
        this.textColor = Color.BLUE;
        setup(parentStage);
    }
    
    /**
     * Creates a simple Toast object that will define how Toast will look.
     * Toast can be then shown with {@link #show(java.lang.String)}.
     * 
     * @param parentStage This is the stage of the parent class.
     * @param toastTime This defines how long will toast be shown with full opacity.
     * @param fadeInDelay This defines how long the fade in animation will last.
     * @param fadeOutDelay This defines how long the fade out animation will last.
     * @param font This defines the font in which text will be displayed.
     * @param textColor This defines the color of the text.
     * @see #ToastNotification(javafx.stage.Stage, int, int, int, javafx.scene.text.Font, javafx.scene.paint.Color) 
     */
    public ToastNotification(Stage parentStage, int toastTime, int fadeInDelay, int fadeOutDelay, Font font, Color textColor) {
        this.toastTime = toastTime;
        this.fadeInDelay = fadeInDelay;
        this.fadeOutDelay = fadeOutDelay;
        this.font = font;
        this.textColor = textColor;
        setup(parentStage);
    }
    
    /**
     * A private method that is called by the 2 constructors. It sets up the stage
     * for Toast notification and its animation.
     * 
     * @param parentStage 
     */
    private void setup(Stage parentStage){
        toastStage=new Stage();
        toastStage.initOwner(parentStage);
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);
        
        text = new Text();
        text.setFont(this.font);
        text.setFill(this.textColor);
        
        StackPane root = new StackPane(text);
        root.setStyle("-fx-background-radius: 20; -fx-background-color: rgba(0, 0, 0, 0.2); -fx-padding: 20px;");
        root.setOpacity(0);
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);
        
        fadeInTimeline = new Timeline();
        KeyFrame fadeInKey1 = new KeyFrame(Duration.millis(fadeInDelay), new KeyValue (toastStage.getScene().getRoot().opacityProperty(), 1)); 
        fadeInTimeline.getKeyFrames().add(fadeInKey1);   
        fadeInTimeline.setOnFinished((ae) -> 
        {
            new Thread(() -> {
                try
                {
                    Thread.sleep(toastTime);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                   Timeline fadeOutTimeline = new Timeline();
                    KeyFrame fadeOutKey1 = new KeyFrame(Duration.millis(fadeOutDelay), new KeyValue (toastStage.getScene().getRoot().opacityProperty(), 0)); 
                    fadeOutTimeline.getKeyFrames().add(fadeOutKey1);   
                    fadeOutTimeline.setOnFinished((aeb) -> toastStage.close()); 
                    fadeOutTimeline.play();
            }).start();
        });

    }
    
    /**
     * Shows text that is passed in as an argument in a Toast which appearance
     * has been defined in constructor.
     * 
     * @param messageText This is the text that will be displayed.
     */
    public void show(String messageText){
        text.setText(messageText);
        toastStage.show();
        fadeInTimeline.play();
    }
    
    /**
     * This method creates a Toast notification and shows it for as long as defined
     * with arguments. This static method creates a new stage and animation for Toast
     * every time, so if you will be using many times you should maybe consider
     * creating an object Toast. In that case you also need to set its characteristics once.
     * 
     * @param parentStage This is the stage of the parent class.
     * @param messageText This is the text that will be displayed.
     * @param toastTime This defines how long will toast be shown with full opacity.
     * @param fadeInDelay This defines how long the fade in animation will last.
     * @param fadeOutDelay This defines how long the fade out animation will last.
     * @see #ToastNotification(javafx.stage.Stage, int, int, int) 
     * @see #ToastNotification(javafx.stage.Stage, int, int, int, javafx.scene.text.Font, javafx.scene.paint.Color) 
     */
    public static void showText(Stage parentStage, String messageText, int toastTime, int fadeInDelay, int fadeOutDelay){
        Stage toastStage=new Stage();
        toastStage.initOwner(parentStage);
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);

        Text text = new Text(messageText);
        text.setFont(Font.font("Verdana", 20));
        text.setFill(Color.BLUE);

        StackPane root = new StackPane(text);
        root.setStyle("-fx-background-radius: 20; -fx-background-color: rgba(0, 0, 0, 0.2); -fx-padding: 20px;");
        root.setOpacity(0);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);
        toastStage.show();

        Timeline fadeInTimeline = new Timeline();
        KeyFrame fadeInKey1 = new KeyFrame(Duration.millis(fadeInDelay), new KeyValue (toastStage.getScene().getRoot().opacityProperty(), 1)); 
        fadeInTimeline.getKeyFrames().add(fadeInKey1);   
        fadeInTimeline.setOnFinished((ae) -> 
        {
            new Thread(() -> {
                try
                {
                    Thread.sleep(toastTime);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                   Timeline fadeOutTimeline = new Timeline();
                    KeyFrame fadeOutKey1 = new KeyFrame(Duration.millis(fadeOutDelay), new KeyValue (toastStage.getScene().getRoot().opacityProperty(), 0)); 
                    fadeOutTimeline.getKeyFrames().add(fadeOutKey1);   
                    fadeOutTimeline.setOnFinished((aeb) -> toastStage.close()); 
                    fadeOutTimeline.play();
            }).start();
        }); 
        fadeInTimeline.play();
    }
}
