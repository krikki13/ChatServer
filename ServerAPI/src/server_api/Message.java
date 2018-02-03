package server_api;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;


/**
 * A generic class that will format the message using given data.<br>
 * It is the only object that can be sent over the Server API, but can carry any object.
 * 
 * @author KRIKKI
 * @version 1
 * @since 23. 6. 2017
 * @param <T> This is the type that you wish to send.
 */
public class Message<T> implements Serializable{
    /**
     * Defines the type of message.
     * 
     */
    public static enum Type implements Serializable{
        /**
         * It should be used when an error has occured on Server and you wish to notify clients about it.
         */
        ERROR,
        /**
         * It should be used to notify clients about something that has happened, but not as a nice user 
         * friendly String (for this just use {@link Type#DATA_STRING}), but as a programmer friendly one so, it can be reacted to with code on client side.
         */
        SYSTEM,
        /**
         * It is sent by Client if login is required. Server will then check if username that was send with that message is still available 
         * and reply with {@link Type#LOGIN_SUCCESSFUL} or {@link Type#LOGIN_DENIED}.
         */
        LOGIN_REQUEST,
        /**
         * It is sent by Server when a login has been successful or if
         * login is not necessary it is send immediately after established connection.
         */
        LOGIN_SUCCESSFUL,
        /**
         * It is sent by Server when a login has not been successful because the username if not available or
         * if maximum amount of clients has been reached.
         */
        LOGIN_DENIED,
        /**
         * It is usually sent as the first message from Server, that carries {@link PublicServerPreferences}, so that Client program
         * can set its Preferences according to Server's.
         */
        PREFERENCES,
        /**
         * It is sent by Client and is used for requesting various informations. Which is specified in message object.
         */
        COMMAND,
        /**
         * It is sent by Client and is used for requesting logoff from Server.
         */
        LOGOFF,
        /**
         * The most commonly used type, it is used for exchanging text messages between Clients and/or Server.
         */
        DATA_STRING,
        /**
         * It is used for exchanging objects other than String between Clients and/or Server, if you send String with Type set to DATA,
         * it will change automatically convert Type to {@link Type#DATA_STRING}.
         */
        DATA;
    }
    
    private static final long serialVersionUID = 0x602E23;
    private final T messageObject; 
    private final String messageSender;
    private final Type messageType;
    private final String[] recipients;
    private final Calendar timeSent;
    private boolean replyAllowed;
    // replyAllowed je zato da se ne bi zgodil, da bi nekdo poslou sporočilo nekomu, ki ne obstaja in se takoj zatem odjavil, potem pa bi dobil nazaj sporočilo
    // da je prejemnik ne obstaja ampak tudi prvotni pošiljatelj ne bi več in bi prišlo do zanke
    
    /**
     * Creates Message that can be sent over the Server API.
     * 
     * @param messageSender This is the username of the sender.
     * @param messageType This is the {@link Type} of the message (if you do not know what, use {@link Type#DATA}).
     * @param messageObject object that you wish to send.
     */
    public Message(String messageSender, Type messageType, T messageObject){
        this.messageObject = messageObject;
        this.messageType = messageType;
        this.messageSender = messageSender;
        this.recipients = null;
        this.replyAllowed = true;
        this.timeSent = Calendar.getInstance();
        
        if(String.class.isInstance(messageObject) && messageType == Type.DATA){
            messageType = Type.DATA_STRING;
        }
        if(!String.class.isInstance(messageObject) && messageType == Type.DATA_STRING){
            throw new ServerException("Message is not String, but message Type was defined to be String");
        }
    } 
   
    /**
     * Creates Message that can be sent over the Server API.
     * 
     * @param messageSender name of the sender.
     * @param messageType This is the {@link Type} of the message (if you do not know what use, {@link Type#DATA}).
     * @param messageObject This is the object that you wish to send.
     * @param recipients This is an array of names of recipients, if some do not exist, sender will receive a warning message, if equals null it will send to all.
     */
    public Message(String messageSender, Type messageType, T messageObject, String[] recipients) {
        this.messageObject = messageObject;
        this.messageType = messageType;
        this.messageSender = messageSender;
        this.recipients = recipients;
        this.replyAllowed = true;
        this.timeSent = Calendar.getInstance();
        
        if(String.class.isInstance(messageObject) && messageType == Type.DATA){
            messageType = Type.DATA_STRING;
        }
        if(!String.class.isInstance(messageObject) && messageType == Type.DATA_STRING){
            throw new ServerException("Message is not String, but message Type was defined to be String");
        }
    }  
    
    /**
     * Creates Message that can be sent over the Server API.
     * 
     * @param messageSender name of the sender.
     * @param messageType This is the {@link Type} of the message (if you do not know what use, {@link Type#DATA}).
     * @param messageObject This is the object that you wish to send.
     * @param recipients This is an array of names of recipients, if some do not exist, sender will receive a warning message, if equals null it will send to all.
     * @param replyAllowed This boolean determines if a reply to this message is allowed.
     */
    public Message(String messageSender, Type messageType, T messageObject, String[] recipients, boolean replyAllowed) {
        this.messageObject = messageObject;
        this.messageType = messageType;
        this.messageSender = messageSender;
        this.recipients = recipients;
        this.replyAllowed = replyAllowed;
        this.timeSent = Calendar.getInstance();
        
        if(String.class.isInstance(messageObject) && messageType == Type.DATA){
            messageType = Type.DATA_STRING;
        }
        if(!String.class.isInstance(messageObject) && messageType == Type.DATA_STRING){
            throw new ServerException("Message is not String, but message Type was defined to be String");
        }
    }

    /**
     * Returns the object that was passed in the message.
     * 
     * @return This is the object.
     */
    public T getMessageObject(){
        return messageObject;
    }
    
    /**
     * Returns the username of the sender.
     * 
     * @return This is the sender's username.
     */
    public String getMessageSender(){
        return messageSender;
    }
    
    /**
     * Returns the type of the message as an enum {@link Type}.
     * 
     * @return This is the sender's username.
     */
    public Type getMessageType() {
        return messageType;
    }
    
    /**
     * Returns the usernames of recipients as an array (It may be null).
     * 
     * @return This is the array of recipients.
     */
    public String[] getRecipients() {
        return recipients;
    }

    /**
     * Returns boolean value if replying to the sender is acceptable or false if undesirable.<br>
     * In some cases this may prevent an endless loop of warning replies.
     * 
     * @return This is the boolean value if replying is desirable.
     */
    public boolean getReplyAllowed() {
        return replyAllowed;
    }
    
    /**
     * Returns {@link java.util.Calendar} of the moment the message was sent,
     * or more precisely when Message was created.
     * 
     * @return This is the time when of sending.
     */
    public Calendar getTimeSent() {
        return timeSent;
    }
    
    /**
     * Sets boolean value if replying to the sender is acceptable or false if undesirable.<br>
     * In some cases this may prevent an endless loop of warning replies.
     * 
     * @param replyAllowed This is the boolean value if replying is desirable.
     */
    public void setReplyAllowed(boolean replyAllowed) {
        this.replyAllowed = replyAllowed;
    }
    
    /**
     * Formats a String suited for command line outputs like: "[12:34:22][John]: Hi Mate!"
     * How time format will look like needs to be passed as a String.<br>
     * Time format is the same as with SimpleDateFormat. It must look something like: dd.MM.yyyy HH:mm:ss
     * 
     * @param timeFormat This is the time format.
     * @return Returns a formatted String.
     * @throws IllegalArgumentException If time format is incorrect.
     */
    public String toStringNicelyWithTime(String timeFormat) throws IllegalArgumentException{
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        return "[%s][%s]: %s".format(formatter.format(timeSent), messageSender, messageObject.toString());
    }
    
    /**
     * Formats a String suited for command line outputs like "[John]: Hi Mate!"
     * 
     * @return Returns a formatted String.
     */
    public String toStringNicely(){
        return "[" + messageSender + "]: "+ messageObject.toString();
    }
    
    /**
     * Formats a String that shows all important Message fields in on place.
     * Might be useful for debugging.
     * 
     * @return Returns a String that shows all important Message fields.
     */ 
    @Override
    public String toString(){
        if(recipients == null)
            return String.format("Sender='%s', Type='%s', Object='%s', Recipients='%s'", this.messageSender, this.messageType.toString(), this.messageObject.toString(), "all");
        else
            return String.format("Sender='%s', Type='%s', Object='%s', Recipients='%s'", this.messageSender, this.messageType.toString(), this.messageObject.toString(), Arrays.toString(recipients));
    }
    
    /**
     * Returns time like specified with argument. <br>
     * Time format is the same as with SimpleDateFormat. It must look something like: dd.MM.yyyy HH:mm:ss
     * 
     * @param timeFormat This is the time format.
     * @return Returns a formatted String.
     * @throws IllegalArgumentException If time format is incorrect.
     */
    public String getTimeAsString(String timeFormat) throws IllegalArgumentException{
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        return formatter.format(timeSent);
    }
}
