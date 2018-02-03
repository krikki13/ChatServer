package server_api;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The main class for the Client program. It connects to {@link Server} and creates {@link ClientMessageReceiver} as a Thread.<br>
 * To get Standard Output from the program override methods {@link #println(String)}, {@link #print(String)}, {@link #errPrintln(String)}.
 * 
 * @author KRIKKI
 * @version 1
 * @since 23.6.2017
 */
public class Client implements Runnable{
    private int serverPort;
    private String serverIP;
    private static String username = "";
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private PublicServerPreferences prefs;
    private String timeFormat = "HH:mm:ss";
    
    /**
     * Sets up necessary things for the client program, however it is still missing the
     * IP address and port number of the server. If connection is attempted before
     * those are set {@link ServerException} will be thrown. You can set them with
     * {@link #serverIP} and {@link #setPort(int)}. 

     * You can start the connection with {@link #run()}.
     * 
     */
    public Client(){
        this.serverPort = -1;
        this.serverIP = "";
    }
    
    /**
     * Sets up necessary things for the client program.
     * This constructor will connect to local server on the same device (IP 127.0.0.1).
     * If you want to connect to device with different IP address, use constructor {@link #Client(String, int)}.
     * You can start the connection with {@link #run()}.
     * 
     * @param serverPort This is the port on which the local server is listening.
     */
    public Client(int serverPort){
        this.serverPort = serverPort;
        this.serverIP = "127.0.0.1";
    }

    /**
     * Sets up necessary things for the client program.
     * You can start the connection with {@link #run()}.
     * 
     * @param serverIP This is the IP address of the server.
     * @param serverPort This is the port on which the server is listening.
     */
    public Client(String serverIP, int serverPort){
        this.serverPort = serverPort;
        this.serverIP = serverIP;
    }
    
    /**
     * Starts connecting to the server. Now how awesome is that. The first message
     * that is received can only be of type {@link Message.Type#PREFERENCES} or 
     * {@link Message.Type#LOGIN_DENIED} if Server has reached maximum amount of connections.
     * 
     * @throws ServerException If IP address or port of the server have not been set.
     */
    @Override
    public void run() throws ServerException{
        // connect to the chat server
        if(this.serverIP.equals("") || this.serverPort < 0) throw new ServerException("Server IP address and port have not been set");
        try {
            println("[system]: Connecting to chat server on "+serverIP+" at "+serverPort+"...");
            socket = new Socket(serverIP, serverPort); // create socket connection
            out = new ObjectOutputStream(socket.getOutputStream()); // create output stream for sending messages
            in = new ObjectInputStream(socket.getInputStream()); // create input stream for listening for incoming messages
            
            while(true){
                Message message;
                try{
                    message = (Message)this.in.readObject();
                }catch(ClassCastException | ClassNotFoundException e){
                    e.printStackTrace();
                    errPrintln("[error]: Object received was not of type Message");
                    continue;
                }
                if(message.getMessageType() == Message.Type.PREFERENCES){
                    setPreferences((PublicServerPreferences)message.getMessageObject());
                    break;
                }else if(message.getMessageType() == Message.Type.LOGIN_DENIED){
                    loginReplyReceived(message);
                    return;
                }
            }
            
            if(prefs.isLoginRequired()) {
                onLoginRequired();
            }
            
            new Thread(new ClientMessageReceiver(this, in)).start(); // create a separate thread for listening to messages from the chat server

            
        } catch (ClassCastException e) {
            errPrintln("[error]: Object sent was not of type Message");
        } catch (Exception e) {
            e.printStackTrace();
            errPrintln("[error]: " + e.toString());
            connectionClosed("Could not connect to server (" + e.getMessage() + ")");
        }
    }
    
    /**
     * Sets server's IP address. This needs to be set before calling the {@link #run()} method,
     * or ServerException will be thrown.
     * 
     * @param ip This is the IP address of the server.
     * @throws ServerException If this method is called when client program is running.
     */
    public void setAddress(String ip) throws ServerException{
        if(this.socket == null || this.socket.isClosed())
            this.serverIP = ip; 
        else
            throw new ServerException("Client is already running");
    }
    
    /**
     * Sets port on which server is listening. This needs to be set before calling the {@link #run()} method,
     * or ServerException will be thrown.
     * 
     * @param port This is the port on which server is listening.
     * @throws ServerException If this method is called when client program is running.
     */
    public void setPort(int port) throws ServerException{
        if(this.socket == null || this.socket.isClosed())
            this.serverPort = port; 
        else
            throw new ServerException("Client is already running");
    }

    /**
     * Closes {@link ObjectOutputStream}, {@link ObjectInputStream} and {@link Socket}.
     * 
     */
    public void close() {
        try {
            if(out != null)
                out.close();
        } catch (NullPointerException | IOException ex) {
        }
        try {
            if(in != null)
                in.close();
        } catch (NullPointerException | IOException ex) {
        }
        try {
            socket.close();
        } catch (NullPointerException | IOException ex) {
        }
    }
    
    /**
     * Returns {@link PublicServerPreferences} that were received
     * from {@link Server}.
     * 
     * @return Returns {@link PublicServerPreferences}.
     */
    public PublicServerPreferences getPublicServerPreferences(){
        return prefs;
    }
    
    /**
     * Attempts to login if server demands so, if not nothing happens.<br>
     * It may return an error (on {@link #errPrintln(java.lang.String)}) if the given username
     * does not abide with regulations.<br>
     * The given username will be sent to {@link Server} and if it does not exist it yet, it will return a success message.
     * 
     * @param newName This is your new username.
     * @see #loginReplyReceived(Message)
     */
    public void login(String newName){
        if(prefs.isLoginRequired() && username.equals("")){ // samo če je loginRequired == true
            if (newName.length() < prefs.getMinUsernameLength()) {
                onLoginDenied("Your name must contain at least "+prefs.getMinUsernameLength()+" signs");
            } else if (newName.length() > prefs.getMaxUsernameLength()) {
                onLoginDenied("Your name is too long (max "+prefs.getMaxUsernameLength()+" characters)");
                // following signs are always forbidden :"',\/<>[]-
            } else if (newName.matches(".*[:\"\'\\[\\]/\\\\<>,-].*")) {
                onLoginDenied("Name must not include following signs :/\\[]\"\' ");
            } else if (newName.toLowerCase().matches("system|error")) {
                onLoginDenied("You cannot use system or error as your username");
            } else if (newName.toLowerCase().matches(prefs.getForbiddenUsernames()) || !newName.matches(prefs.getAllowedUsernames())) {
                onLoginDenied("This name is not allowed");
            } else {
                try {
                    out.writeObject(new Message<>(newName, Message.Type.LOGIN_REQUEST, "")); // send the message to the chat server
                    out.flush(); // ensure the message has been sent
                } catch (Exception e) {
                    println("[error]: An unknown error has occured. Please try again");
                }
            }
        }
    }
    
    /**
     * Is meant to be called by ClientMessageReceiver when it receives a message with type 
     * LOGIN_SUCCESSFUL or LOGIN_DENIED and it then takes action on the result.
     * 
     * @param message This is the message that was received.
     * @throw ServerException ServerException is thrown if an incorrect message arrives.
     * @see #login(java.lang.String) 
     */
    void loginReplyReceived(Message<?> message) throws ServerException{
        if (username.equals("")) {
            if (message.getMessageType() == Message.Type.LOGIN_DENIED) {
                onLoginDenied(message.getMessageObject().toString());
                connectionClosed("");
            } else if (message.getMessageType() == Message.Type.LOGIN_SUCCESSFUL) {
                if(message.getRecipients().length != 1)
                    throw new ServerException("Too many recipients for a LOGIN_SUCCESSFUL type of message ("+message.getRecipients().length+")");
                username = message.getRecipients()[0];
                
                onLoginSuccessful(message.getMessageObject().toString());
            }
        }
    }
    
    /**
     * This method is called when message of type {@link server_api.Message.Type#SYSTEM}
     * is received.
     * 
     * @param description This is the type description of the message.
     * @param value This is the value of the message.
     */
    protected final void systemMessageReceived(String description, String value){
        switch(description){ // it needs to be final method, because otherwise noone would call onUserDisconnected(String) and onNewUserConnected(String)
            case "user-disconnect":
                onUserDisconnected(value);
                break;
            case "user-connect":
                onNewUserConnected(value);
                break;
            case "ban":
                connectionClosed(value);
                break;
            default:
                onSystemMessageReceived(description, value);
        }
    }
    
    /**
     * This method is called when message of type {@link server_api.Message.Type#SYSTEM}
     * is received. It can be overriden so that different response can be issued for the 
     * message received. System messages that invoke this method are recipients-not-exist and
     * connected-clients.
     * 
     * @param description This is the type description of the message.
     * @param value This is the value of the message.
     */
    protected void onSystemMessageReceived(String description, String value){
        switch(description){
            case "recipients-not-exist":
                errPrintln("[error]: Following recipients do not exist: " + value);
                break;
            case "connected-clients":
                println("[system]: Currently connected clients are: " + value);
                break;
            default:
                errPrintln("[error]: System message not recognized");
        }
    }
    
    /**
     * Returns this client's username. If client is not logged in yet, it returns "",
     * or if login is not required it returns port with colon at the beginning.
     * 
     * @return Returns the username of this client.
     */
    public String getUsername(){
        return username;
    }
    
    /**
     * Returns this client's port.
     * 
     * @return Returns
     */
    public int getPort(){
        return this.socket.getPort();
    }
    
    /**
     * Logoff from the server. This method sends a message to {@link Server} with
     * {@link Message.Type#LOGOFF} and also calls method {@link #connectionClosed}.
     */
    public void logoff(){
        try{
            sendMessage(new Message<>(username, Message.Type.LOGOFF, ""));
        }catch(NullPointerException e){}
        username = "";
        connectionClosed("Logoff successful");
    }
        
    /**
     * Sends the message as command.
     * Depending on the command type, reply might be received of type {@link server_api.Message.Type#SYSTEM},
     * {@link server_api.Message.Type#DATA_STRING} or {@link server_api.Message.Type#ERROR}.
     * 
     * @param msg This is the command to be sent.
     */        
    public void sendCommand(String msg) {
        if(!msg.equals("") && !msg.matches(prefs.forbiddenWords) && !msg.startsWith("/"))
            sendMessage(new Message<>(username, Message.Type.COMMAND, msg, null));

    }
    
    /**
     * Sends the message given as a String.      
     * If user is required to login, but has not been done yet,
     * this method will attempt to login instead of sending message. If message starts with slash /, it will be sent as a command.
     * If you want to send to only some clients, use {@link #sendText(java.lang.String, java.lang.String[])}.
     * 
     * @param msg This is the message to be sent.
     * @see #login(String)
     * @see #sendCommand(String)
     */        
    public void sendText(String msg) {
        if (username.equals("") && prefs.isLoginRequired()) 
            login(msg);
        else{
            if(msg.equals("") || msg.matches(prefs.forbiddenWords)) return; 
            if(msg.startsWith("/"))
                sendMessage(new Message<>(username, Message.Type.COMMAND, msg, null));
            else
                sendMessage(new Message<>(username, Message.Type.DATA_STRING, msg, null));
        }
    }
    /**
     * Sends the message given as a String to recipients specified in array. If array 
     * is null, it will send message to everyone. If message starts with slash /, it will be sent as a command.
     * Remember sending to all will cause you to receive your message as well, but now only clients
     * in array will receive the message (so if you want to get confirmation add yourself to recipients).
     * 
     * @param msg This is the message to be sent.
     * @param recipients This is the array of recipients for this message.
     */        
    public void sendText(String msg, String[] recipients) {
        if (msg.equals("") || msg.matches(prefs.forbiddenWords))  return;
        if(msg.startsWith("/"))
            sendMessage(new Message<>(username, Message.Type.COMMAND, msg, null));
        else
            sendMessage(new Message<>(username, Message.Type.DATA_STRING, msg, recipients));
    }
    /**
     * Sends the message given as any Object. 
     * If you want to send to only some clients, use {@link #sendObject(java.lang.Object, java.lang.String[]) }.
     * 
     * @param msg This is the message to be sent.
     */ 
    public void sendObject(Object msg) {
        sendMessage(new Message<>(username, Message.Type.DATA, msg, null));
    }
    /**
     * Sends the message given as any Object to recipients specified in array. If array 
     * is null, it will send message to everyone.
     * Remember sending to all will cause you to receive your message as well, but now only clients
     * in array will receive the message (so if you want to get confirmation add yourself to recipients).
     * 
     * @param msg This is the message to be sent.
     * @param recipients This is the array of recipients for this message.
     */ 
    public void sendObject(Object msg, String[] recipients) {
        sendMessage(new Message<>(username, Message.Type.DATA, msg, recipients));
    }
    
    /**
     * Sends the message given as {@link server_api.Message} object.
     * If recipients array in {@link server_api.Message} equals null, it sends to everyone, however if recipients
     * equals empty array it will not send to anyone.
     * 
     * @param message This is the {@link Message} to be sent.
     */ 
    public void sendMessage(Message<?> message) {
        if (username.equals("") && prefs.isLoginRequired() && message.getMessageType() != Message.Type.LOGOFF) {
            errPrintln("[error]: You need to login first");
            return;
        }
        if(this.socket == null || this.socket.isClosed()) return;
        try {
            out.writeObject(message); // send the message to the server
            out.flush(); // ensure the message has been sent
        } catch (IOException | ServerException ex) {
            errPrintln("[system]: Could not send message");
            ex.printStackTrace(System.err);
        }
    }
       
    /**
     * Returns time like specified with argument. <br>
     * Time format is the same as with SimpleDateFormat. It must look something like: dd.MM.yyyy HH:mm:ss
     * 
     * @param timeFormat This is the time format.
     * @return Returns a formatted String.
     * @throws IllegalArgumentException If time format is incorrect.
     */
    public String timeToString(String timeFormat) throws IllegalArgumentException{
        SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
        return formatter.format(new Date());
    }
    
    /**
     * This is called whenever your connection with server is closed or lost. IT
     * It then calls the public method {@link #onConnectionClosed(java.lang.String)}
     * that is meant to be overriden. This on takes care of things that must
     * be done for proper working. It will also call the {@link #close()} method.
     * <br>
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param reason This is the reason why connection was closed.
     */
    protected final void connectionClosed(String reason){
        close();
        username = "";
        socket = null;
        out = null;
        in = null;
        onConnectionClosed(reason);
    }
    
    /**
     * This method is called whenever your connection with server is closed or lost. However you should not call it directly,
     * but call method {@link #connectionClosed(java.lang.String)} instead and override this one.
     * By default it will print out a message that requests a login through {@link #println(java.lang.String)} method.
     * It is meant to be overriden so user can do something on that event.<br>
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param reason This is a description of reason why it was closed. 
     */
    public void onConnectionClosed(String reason){
        println(reason);
    }
    
    /**
     * This method is called when you connect and the server demands a login.
     * By default it will print out a message that requests a login through {@link #println(java.lang.String)} method.
     * It can be overriden so user can do something else on that event.<br>
     * A better way would be using actual events but I don't know how to do that.
     */
    public void onLoginRequired(){
        println("[system]: You have to login");
    }
    
    /**
     * This method is called when login was successful. It receives an argument with message about success.
     * By default it will print out a message that requests a login through {@link #println(java.lang.String)} method.
     * It can be overriden so user can do something else on that event.<br>
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param message This is a message about success.
     */
    public void onLoginSuccessful(String message){
        println("[system]: "+ message);
    }
    
    /**
     * This method is called when you connect and the server rejects the login. It receives an argument with message about rejection.
     * By default it will print out a message that requests a login through {@link #println(java.lang.String)} method.
     * It can be overriden so user can do something else on that event.<br>
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param message This is a message that describes reason of rejection.
     */
    public void onLoginDenied(String message){
        println("[system]: "+ message);
    }
    
    /**
     * This method is called when a new user connects. It receives an argument with the username of the client.
    * By default it will print out a message about the event through {@link #println(java.lang.String)} method.
     * It is meant to be overriden so user can do something on that event.<br>
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param username This is the username of the new user.
     */
    public void onNewUserConnected(String username){
        println("[system]: User " + username + " has connected");
    }

    /**
    * This method is called when a user disconnects. It receives an argument with the username of the client.
    * By default it will print out a message about the event through {@link #println(java.lang.String)} method.
    * It is meant to be overriden so user can do something on that event.<br>
    * A better way would be using actual events but I don't know how to do that.
    * 
    * @param username This is the username of the user that disconnected.
    */
    public void onUserDisconnected(String username){
        println("[system]: User " + username + " has disconnected");
    }
    
    /**
     * It prints the output to Standard Output, if you wish to print elsewhere simply override the method.
     * You can also override {@link #print(String)} and {@link #errPrintln(String)}.
     * 
     * @param s This is the output text.
     */
    public void println(String s){
        System.out.println("["+timeToString(timeFormat)+"]"+s);
    }
    
    /**
     * It prints the output to Standard Output, if you wish to print elsewhere simply override the method.
     * You can also override {@link #println(String)} and {@link #errPrintln(String)}.
     * 
     * @param s This is the output text.
     */
    public void print(String s){
        System.out.print("["+timeToString(timeFormat)+"]"+s);
    }
    
    /**
     * It prints the output to Standard Output, if you wish to print elsewhere simply override the method.
     * You can also override {@link #println(String)} and {@link #print(String)}.
     * 
     * @param s This is the output text.
     */
    public void errPrintln(String s){
        System.err.println("["+timeToString(timeFormat)+"]"+s);
    }

    /**
     * Sets PublicServerPreferences, that it usually receives as the first message after
     * establishing connection.
     * 
     * @param prefs This is the object PublicServerPreferences that specifies what Server demands.
     */
    void setPreferences(PublicServerPreferences prefs) {
        this.prefs = prefs;
    }
    
    
}
