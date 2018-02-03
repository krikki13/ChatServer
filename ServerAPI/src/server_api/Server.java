package server_api;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The main class for Server API. 
 * It sets up all necessary things for a server program and provides useful methods.<br>
 * It requires an object {@link PrivateServerPreferences} that will define how Server should work.<br>
 * To get Standard Output from the program override methods {@link #println(String)}, {@link #print(String)}, {@link #errPrintln(String)}.
 * 
 * @author KRIKKI
 * @version 1.0
 * @since 23.6.2017
 */
public class Server implements Runnable{
    private ServerSocket serverSocket;
    private HashMap <String, ServerConnector> clients;
    private HashMap <String, ServerConnector> clientsLoggingIn; // only if login is required
    private PrivateServerPreferences preferences;
    boolean requestToStop = false;
    
    // when number of clients reaches maximum, server will still be listening for
    // new connections, but it will send LOGIN_DENIED (no matter what value is loginRequired)
    
    /**
     * Sets up necessary things for server that are read from {@link PrivateServerPreferences} object.<br>
     * If {@link PrivateServerPreferences} has not been setup completely (some variables have not been setup yet) it will
     * throw ServerException.
     * 
     * @param prefs This is the object of preferences for server.
     */
    public Server(PrivateServerPreferences prefs){  
        preferences = prefs;
        this.clients = new HashMap<>();
        this.clientsLoggingIn = new HashMap<>();
    }
    
    /**
     * Sets up necessary things for server. It is still missing {@link PrivateServerPreferences} object that needs to
     * be set using {@link #setPrivateServerPreferences(server_api.PrivateServerPreferences)} before starting server with {@link #run()},
     * otherwise a ServerExcepiton will be thrown.
     * 
     * @see #setPrivateServerPreferences(server_api.PrivateServerPreferences) 
     */
    public Server(){  
        preferences = null;
        this.clients = new HashMap<>();
        this.clientsLoggingIn = new HashMap<>();
    }
    
    /**
     * It sets up necessary {@link PrivateServerPreferences}, without which Server cannot be started.
     * 
     * @param prefs This is the {@link PrivateServerPreferences} that specifies how server should behave.
     */
    public void setPrivateServerPreferences(PrivateServerPreferences prefs){
        preferences = prefs;
    }
    
    /**
     * Starts the Server. It is very important that {@link PrivateServerPreferences} have been set. Otherwise 
     * ServerException will be thrown.
     * 
     * @throws ServerException If {@link PrivateServerPreferences} have not been set correctly.
     */
    @Override
    public void run() throws ServerException{
        requestToStop = false;
        if(preferences == null)
            throw new ServerException("PrivateServerPreferences are null");
        else if (!preferences.isValid())
            throw new ServerException("PrivateServerPreferences have not been set correctly");
                
        try{ // create new serverSocket
            serverSocket = new ServerSocket(this.preferences.getPort()); 
        } catch (IOException ioException){
            ioException.printStackTrace();
            return;
        }
        
        // start listening for new connections
        println("[system]: Listening at " + this.serverSocket.getLocalPort() + "...");
        onServerStarted();
        try {
            while (!requestToStop) {
                Socket newClientSocket = serverSocket.accept(); // wait for a new client connection

                // to many clients, deny new request
                if(clients.size() + clientsLoggingIn.size() >= this.preferences.getMaxNumberOfClients()){
                    ObjectOutputStream output = new ObjectOutputStream(newClientSocket.getOutputStream()); // create input stream for listening for incoming messages
                    Message<String> denialMessage = new Message<>("error", Message.Type.LOGIN_DENIED, "Connection denied due to too many connected clients", new String[]{""+newClientSocket.getPort()});
                    output.writeObject(denialMessage);
                    output.flush();
                    println("[system]: User at port "+newClientSocket.getPort()+" has been denied because the maximum amount of clients has been reached");
                }else{
                    int newPort = newClientSocket.getPort();
                    if(this.preferences.isLoginRequired()){
                        if(addClientLoggingIn(newPort, new ServerConnector(this, newClientSocket)))
                            new Thread(clientsLoggingIn.get(":"+newPort)).start();
                        onNewConnectionOpened(newClientSocket.getPort());
                    }else{
                        if(addClient(":"+newPort, new ServerConnector(this, newClientSocket)))
                            new Thread(clients.get(":"+newPort)).start();
                        onNewConnectionOpened(newClientSocket.getPort());
                    }
                }
            }
            if(requestToStop){
                println("[system]: Server stopped by user");
            }
        } catch(java.net.BindException e){
            errPrintln("Address already in use: JVM_Bind");
        }catch(Exception e) {
            if(e.getMessage() == null){
                errPrintln(e.toString());
            }else if(!e.getMessage().equals("socket closed")){
                errPrintln("[error]: Accept failed.");
                e.printStackTrace(System.err);
            }
        }
        
    }
    
    
    
    /**
     * Closes all running resources and stops server.
     * 
     */
    public void stop(){
        try{
            if(serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
            
        }catch(Exception e){}
        serverSocket = null;
        
        for(ServerConnector conn: clients.values()){
            conn.close();
        }
        clients.clear();
        clientsLoggingIn.clear();
        requestToStop = true;
    }   
    
    /**
     * Returns the IP address of the server.
     * 
     * @return Returns the IP address of the server.
     */
    public String getServerIPAddress(){
        return serverSocket.getInetAddress().getHostAddress();
    }
    
    /**
     * Returns the port number on which the server is listening.
     * 
     * @return Returns the port number of the server.
     */
    public int getServerPort(){
        return serverSocket.getLocalPort();
    }
    
    /**
     * Returns {@link PublicServerPreferences} intended for sharing with clients.<br>
     * It may return null if {@link PrivateServerPreferences} have not been set.
     * 
     * @return It returns PublicServerPreferences object.
     */
    public PublicServerPreferences getPublicServerPreferences(){
        if(preferences == null) return null;
        return preferences.forPublic();
    }
    
    /**
     * Returns {@link PrivateServerPreferences}.<br>
     * It may return null if {@link PrivateServerPreferences} have not been set.
     * 
     * @return It returns PrivateServerPreferences object.
     */
    public PrivateServerPreferences getPrivateServerPreferences(){
        if(preferences == null) return null;
        return preferences;
    }
    
    /**
     * Receives a command that must start with a slash /. You can use following commands:
     * /who, /ban, /stop, /help.
     * 
     * @param command This is the command.
     */
    public void serverCommand(String command){
        if(!command.startsWith("/")) return;
        String[] comm = command.split(" ");
        switch(comm[0]){
            case "/who":
                if(comm.length==1){
                    println("[server]: Connected clients: " + printSet(clients.keySet(), ", "));
                }else if(comm.length == 2){
                    if(comm[1].startsWith("-")){
                        if(comm[1].equals("-l")){
                            println("[server]: Clients logging in: " + printSet(clientsLoggingIn.keySet(), ", "));
                        }else if (comm[1].equals("-a")){
                            println("[server]: All clients: " + printSet(clients.keySet(), ", ") + "; " + printSet(clientsLoggingIn.keySet(), ", "));
                        }else if(comm[1].equals("-?")){
                            println("[server]: /who: Shows connected clients (not the ones that are currently logging in). You can also use switches:\n  -l : Shows only the clients that are logging in\n  -a : Shows all the clients (also ones that are logging in)");
                        }else{
                            println("[server]: /who: Unknown switch. Use -l or -a, or use -? for help");
                        }
                    }else{
                        if(clients.containsKey(comm[1]) || clientsLoggingIn.containsKey(comm[1])){
                            println("[server]: "+comm[1] + ", IP address: " + getClientAddress(comm[1]) + ", Port: " + getClientPort(comm[1]));
                        }else
                            println("[server]: Client with username " + comm[1] + " does not exist (for ports use colon at the beginning)");
                    }
                }else{
                    println("[server]: /who: Too many arguments");
                }
                break;
            case "/stop":
                if(comm.length == 1)
                    this.stop();
                else if(comm.length == 2 && comm[1].equals("-?"))
                    println("[system]: /stop: Stops server, if it is running at the moment");
                break;                
            case "/ban":
                if(comm.length == 1){
                    println("[system]: /ban: Not enough arguments for command. Use switch -? for help");
                }else if(comm.length == 2 && comm[1].equals("-?")){
                    println("[system]: /ban: Bans the client specified with an argument. If more arguments are given the following will be used as a reason and will be sent to the client.");
                }else if(comm.length >= 2 && !comm[1].startsWith("-")) {
                    String subArray = "";
                    for (int i = 2; i < comm.length; i++) 
                        subArray = comm[i] + " ";
                    banClient(comm[1], subArray);
                }else{
                    println("[server]: /ban: Unknown switch. Use -? for help");
                }
                break;
            case "/help":
                if(comm.length == 1){
                    println("[server]: /help: Supported commands:\n  /who : prints out connected users\n  /ban : bans user specified as an argument\n  /stop : stops the server");
                }else{
                    println("[server]: /help: Command takes no arguments. If you want more information about specific command, use switch -? at that command");
                } 
            default:
                println("[server]: Unknown command " + comm[0]);
                
        }
    }
       
    /**
     * Sends the message to clients that were passed to {@link Message} when it was created.<br>
     * If some clients do not exist, it will return their usernames in a String.<br>
     * If the array in {@link Message} equals null to all clients.
     * 
     * @param message This is the message that will be send to clients.
     * @return It returns names of clients that it could not sent message to, if any are specified in Message.
     * @throws ServerException If message could not be sent.
     */
    public String[] sendToClients(Message<?> message) throws ServerException{
        if(message.getRecipients() == null){
            sendToAllClients(message);
            return new String[0];
        }else{
            return sendToSomeClients(message);
        }
    }
    
    /**
     * Private method that is called by sendToClients(Message).<br>
     * Sends the message to all the clients specified in HashMap clients.
     * 
     * @param message This is the message to be sent.
     * @throws ServerException If message could not be sent.
     */
    private void sendToAllClients(Message<?> message) throws ServerException {
        Iterator<String> i = clients.keySet().iterator();
        System.out.println("ClientsLoggingIn: "+clientsLoggingIn);
        System.out.println("Clients:          "+clients);
        while (i.hasNext()) { // iterate through the client list
            try {
                String s=i.next();
                System.out.println("s: "+s);
                if(clients.get(s) != null)  {
                    System.out.println("   also not null");
                    ObjectOutputStream output = clients.get(s).output;
                    output.writeObject(message);
                    output.flush();
                }
            } catch(ConcurrentModificationException | NullPointerException | NotActiveException ex){
                ex.printStackTrace();
            }catch (Exception e) {
                errPrintln("[system]: Could not send message to a client");
                e.printStackTrace(System.err);
            }
        }
    }
    
    /**
     * Private method that is called by sendToClients(Message).<br>
     * Sends the message to clients specified in array Message.recipients if they exist
     * in HashMap clients.<br>
     * Returns the ones that could not send to as a String array.
     * 
     * @param message This is the message to be sent.
     * @return It returns names of clients that it could not sent message to (or an empty array).
     * @throws ServerException If message could not be sent.
     */
    private String[] sendToSomeClients(Message<?> message) throws ServerException {
        ArrayList<String> failedRecipients = new ArrayList<>();
        for(String recipient: message.getRecipients()){
            if(clients.containsKey(recipient)){
                try {
                    if(clients.get(recipient) != null)
                        clients.get(recipient).output.writeObject(message);
                } catch (Exception e) {
                    errPrintln("[system]: Could not send message to a client");
                    e.printStackTrace(System.err);
                }
            }else{
                failedRecipients.add(recipient);
            }
        }
        if(failedRecipients.isEmpty()) return new String[0];
        return failedRecipients.toArray(new String[0]);
    }
    
    /**
     * This method is sends to all clients except the once specified in given array.
     * Recipients in {@link server_api.Message} will in this case be ignored.
     * 
     * @param message This is the message to be sent.
     * @param dontSendTo This is the array which with usernames to which message will not be sent.
     * @throws ServerException If message could not be sent.
     */
    public void sendToOtherClients(Message<?> message, String[] dontSendTo) throws ServerException {
        List<String> asList = Arrays.asList(dontSendTo);
        Set<String> sendTo = new HashSet<>(clients.keySet());
        sendTo.removeAll(asList);
        for(String recipient: sendTo){
            if(clients.containsKey(recipient)){
                try {
                    if(clients.get(recipient) != null)
                        clients.get(recipient).output.writeObject(message);
                } catch (Exception e) {
                    errPrintln("[system]: Could not send message to a client");
                    e.printStackTrace(System.err);
                }
            }
        }
    }
    
    /**
     * Returns a Set of usernames of clients.
     * 
     * @return Returns a Set of usernames of clients.
     */
    public Set<String> getClients() {
        return clients.keySet();
    }
    
     /**
     * Returns a Set of usernames of clients currently logging in.
     * 
     * @return Returns a Set of usernames of clients currently logging in.
     */
    public Set<String> getClientsLoggingIn() {
        return clientsLoggingIn.keySet();
    }
    
    /**
     * Returns the IP address of the selected client. If client has not yet logged in,
     * you can pass in port number with colon in front of it (:1234).
     * It returns "", if client was not found.
     * 
     * @param username This is the username of the client.
     * @return Returns the IP address of the client.
     */
    public String getClientAddress(String username){
        if(clientsLoggingIn.containsKey(username))
            return clientsLoggingIn.get(username).socket.getInetAddress().getHostName();
        if(clients.containsKey(username))
            return clients.get(username).socket.getInetAddress().getHostName();
        return "";
    }
    
    /**
     * Returns the port of the selected client. If client has not yet logged in,
     * you can pass in port number with colon in front of it (:1234).
     * It returns -1, if client was not found.
     * 
     * @param username This is the username of the client.
     * @return Returns the port of the client.
     */
    public int getClientPort(String username){
        if(clientsLoggingIn.containsKey(username))
            return clientsLoggingIn.get(username).socket.getPort();
        if(clients.containsKey(username))
            return clients.get(username).socket.getPort();
        return -1;
    }
    
    /**
     * Adds ServerConnector object that is communicating with the client to HashMap
     * that is used only for new connections and only if login is required.
     * When login is successful, client should be removed from clientsLoggingIn Map
     * and placed to clients Map. This is best done with {@link #clientLoggedIn(java.lang.String, int, server_api.ServerConnector)}.
     * 
     * @param port This is the port on which the client is connected.
     * @param serverConnector This is the {@link ServerConnector} object communicating with the client.
     * @return Returns true if adding was successful.
     */
    boolean addClientLoggingIn(int port, ServerConnector serverConnector){
        synchronized(this) {
            if(clientsLoggingIn.containsKey(":"+port)){
                throw new RuntimeException("Holy crap! That is f*cking impossible");
            }
            clientsLoggingIn.put(":"+port, serverConnector);
        }
        return true;
    }
    
    /**
     * This method should only be used if login is required and only when client
     * has successfully logged in with new username. It removes client from Map of clients
     * if login phase to Map of connected clients.
     * 
     * @param username This is the new username of the client.
     * @param port This is the port on which client is connected.
     * @param serverConnector This is the {@link ServerConnector} object communicating with the client.
     * @return Returns true if operation was successful.
     */
    boolean clientLoggedIn(String username, int port, ServerConnector serverConnector){
        synchronized(this){
            if(addClient(username, serverConnector)){
                removeClientLoggingIn(":"+port);
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * It removes a client from the Map of clients logging in at the moment.
     * It automatically adds the necessary colon in front of the port.
     * 
     * @param port This is the port of the client.
     * @return It returns true, if it was successful.
     */
    public boolean removeClientLoggingIn(int port) {
        synchronized(this) {
            if(clientsLoggingIn.containsKey(":"+port)){
                clientsLoggingIn.remove(":"+port);
                return true;
            }
            return false;
        }
    }
    
    /**
     * It removes a client from the Map of clients logging in at the moment.
     * It takes port as a String with colon at the beginning like ":1234".
     * 
     * @param port This is the port of the client.
     * @return It returns true, if it was successful.
     */
    public boolean removeClientLoggingIn(String port) {
        synchronized(this) {
            if(clientsLoggingIn.containsKey(port)){
                clientsLoggingIn.remove(port);
                return true;
            }
            return false;
        }
    }
    
    /**
     * This adds a new client to the Map.
     * 
     * @param username This is the username of the new client.
     * @param serverConnector This is {@link ServerConnector} object that is handling communication with the new client.
     * @return It returns true if it was successful.
     */
    boolean addClient(String username, ServerConnector serverConnector) {
        synchronized(this) {
            if(!clients.containsKey(username)){
                clients.put(username, serverConnector);
                return true;
            }
            return false;
        }
    }
    
    
    
    /**
     * It removes a client from the Map by username. If you want to remove client that was logging
     * in and you specify port you must always put colon in front of the port.<br>
     * If you want to stop connection with client and maybe send message to them you should use {@link #banClient(java.lang.String, java.lang.String) }
     * 
     * @param username This is the username of the client.
     * @return It returns true, if it was successful.
     */
    public boolean removeClient(String username) {
        if(username.startsWith(":") && isLoginRequired()){
            synchronized(this) {
                if(clientsLoggingIn.containsKey(username)){
                    clientsLoggingIn.remove(username);
                    return true;
                }
                return false;
            }
        }else{
            synchronized(this) {
                if(clients.containsKey(username)){
                    clients.remove(username);
                    return true;
                }
                return false;
            }
        }
    }
    
    /**
     * Closes input and output streams of the selected client and also removes it from the Map.<br>
     * If second argument does not equal "", the client will receive a message with given String, for example the reason
     * why he was banned.
     * 
     * @param username This is the username of the client to be banned.
     * @param reason This is the reason client will receive as a message. If empty, nothing will be sent.
     */
    public void banClient(String username, String reason){
        ServerConnector connector;
        if(username.startsWith(":") && isLoginRequired())
            connector = clientsLoggingIn.get(username);
        else
            connector = clients.get(username);
        int port = connector.socket.getPort();
        if(reason != null && !reason.equals("")){
            try {
                synchronized(this){
                    ObjectOutputStream ous = connector.output;
                    ous.writeObject(new Message<>("system", Message.Type.SYSTEM, "ban=\""+reason+"\"", new String[]{username}));
                    ous.flush();
                }
            } catch (IOException ex) {
                System.out.println("Could not send message");
            }
        }
        connector.close();
        removeClient(username);
        onConnectionClosed(username, port, "ban");
        sendToAllClients(new Message<>("system", Message.Type.SYSTEM, "user-disconnect=\""+username+"\"", null));
    }
    
    /**
     * Returns true if user needs to login before doing anything else.
     * 
     * @return Returns true if login is necessary.
     * @see Client#login(String)
     */
    public boolean isLoginRequired(){
        return this.preferences.isLoginRequired();
    }

    /**
     * Returns the maximum amount of clients connected at once.
     * 
     * @return Returns the maximum amount of clients.
     */
    public int getMaxNumberOfClients() {
        return this.preferences.getMaxNumberOfClients();
    }

    /**
     * This method is called when the server starts listening for connections.
     * It is meant to be overriden so user can do something on that event.
     * A better way would be using actual events but I don't know how to do that.
     * 
     */
    public void onServerStarted(){
    }
    
     /**
     * This method is called whenever a new connection is made. After successful login
     * also {@link #onSuccessfulLogin(java.lang.String, int)} is called.
     * If login is not required it is called almost simultaneously as {@link #onNewConnectionOpened(int)}.<br>
     * It is meant to be overriden so user can do something on that event.
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param newUserPort This is the port to which the new client has connected.
     * 
     */
    public void onNewConnectionOpened(int newUserPort){
    }
    
    
    /**
     * This method is called whenever a login is successful. 
     * If login is not required it is called almost simultaneously as {@link #onNewConnectionOpened(int)}.<br>
     * It is meant to be overriden so user can do something on that event.
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param username This is the username of the new client.
     * @param port This is the port of the new client.
     */
    public void onSuccessfulLogin(String username, int port){
    }
    
    /**
     * This method is called whenever a connection is closed for some reason. 
     * The reason is described as: logoff, ban or unknown.
     * By default it prints a message describing event throguh {@link #println(java.lang.String)} method.<br>
     * It is meant to be overriden so user can do something on that event.
     * A better way would be using actual events but I don't know how to do that.
     * 
     * @param username This is the username of the client that disconnected.
     * @param port This is the port of the client that disconnected.
     * @param description This is a String that describes the reason.
     */
    public void onConnectionClosed(String username, int port, String description){
        String addition="";
        if(username.startsWith(":")) // so that is says "user john has blah blah" or "user at :1234 has blah blah"
            addition = "at ";
        switch(description){
            case "logoff": 
                println("[system]: User " + addition + username + " has logged off"); break;
            case "ban": 
                println("[system]: User " + addition + username + " has been banned"); break;
            default:
                println("[system]: User " + addition + username + " has disconnected"); break;
        }
    }
    
    /**
     * Returns the socket of user specified by username.
     * 
     * @param username This is the username of the client.
     * @return Returns socket of the user, if the user does not exist null.
     */
    public Socket getUserSocket(String username){
        return clients.get(username).socket;
    }
    
    /**
     * It prints the output to Standard Output, if you wish to print elsewhere simply override the method.<br>
     * You can also override {@link #print(String)} and {@link #errPrintln(String)}.
     * 
     * @param s This is the output text.
     */
    public void println(String s){
        System.out.println(s);
    }
    
    /**
     * It prints the output to Standard Output, if you wish to print elsewhere simply override the method.<br>
     * You can also override {@link #println(String)} and {@link #errPrintln(String)}.
     * 
     * @param s This is the output text.
     */
    public void print(String s){
        System.out.print(s);
    }
    
    /**
     * It prints the output to Standard Output, if you wish to print elsewhere simply override the method.<br>
     * You can also override {@link #println(String)} and {@link #print(String)}.
     * 
     * @param s This is the output text.
     */
    public void errPrintln(String s){
        System.err.println(s);
    }
    
    /**
     * Prints String[] nicely, separated by the second argument.
     * 
     * @param array This is the String array you want to print.
     * @param separator This defines, how elements will be separated.
     * @return Returns String representing the values of the array.
     */
    public static String printArray(String[] array, String separator){
        if(array == null || array.length == 0) return "";
        
        String toPrint="";
        for(String s: array){
            toPrint += s + separator;
        }
        return toPrint.substring(0, toPrint.length()-separator.length());
    }
    
    /**
     * Prints String Set nicely, separated by the second argument.
     * 
     * @param set This is the Set you want to print.
     * @param separator This defines, how elements will be separated.
     * @return Returns String representing the values of the Set.
     */
    public static String printSet(Set<String> set, String separator){
        if(set.isEmpty()) return "";
        
        String toPrint="";
        for(String s: set){
            toPrint += s + separator;
        }
        return toPrint.substring(0, toPrint.length()-separator.length());
    }
}
