package server_api;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;


/**
 * Is used by the Server class and is used as a connection handler for every
 * individual client.
 *
 * @author KRIKKI
 * @version 1
 * @since 23. 6. 2017
 */
public class ServerConnector implements Runnable {
    /**
     * This is the {@link java.net.Socket} ServerConnector
     * is using for connection with Client.
     */
    public Socket socket;
    /**
     * This is the stream, through which ServerConnector will be receiving
     * messages from Client.
     */
    public ObjectInputStream input;
    /**
     * This is the stream, through which ServerConnector will be sending
     * messages to Client.
     */
    public ObjectOutputStream output;
    private Server server;
    private String username = "";
    private boolean connected = false;

    /**
     * It sets up necessary things.
     *
     * @param server This is the {@link Server} object with which
     * ServerConnector will be communicating.
     * @param socket This is the {@link java.net.Socket} which connects to the
     * new user.
     */
    public ServerConnector(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    /**
     * Starts receiving messages.
     *
     * @throws ClassCastException If an Object other than of type
     * {@link Message} arrives.
     * @throws ServerException If more clients attempt to connect to the same
     * port.
     */
    @Override
    public void run() throws ClassCastException, ServerException {
        println("[system]: Connected with " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort());
        connected = true;
        try {
            output = new ObjectOutputStream(this.socket.getOutputStream()); // create input stream for listening for incoming messages
            output.flush();

            input = new ObjectInputStream(this.socket.getInputStream()); // create input stream for listening for incoming messages
        } catch (IOException e) {
            errPrintln("[system]: Could not open input stream!");
            e.printStackTrace(System.err);
            connected = false;
            this.server.removeClientLoggingIn(this.socket.getPort());
            return;
        }
        try {
            output.writeObject(new Message<>("system", Message.Type.PREFERENCES, server.getPublicServerPreferences(), new String[]{":" + this.socket.getPort()}));
        } catch (IOException ex) {
            System.out.println("error");
        }

        // if server does not require login, LOGIN_SUCCESSFUL will be sent automatically as soon connection is established
        if (!server.isLoginRequired()) {
            int port1 = this.socket.getPort();
            username = ":" + port1;
            Message<String> message = new Message<>("system", Message.Type.LOGIN_SUCCESSFUL, "Login was successful", new String[]{username});
            server.onSuccessfulLogin(username, port1);
            server.sendToOtherClients(new Message<>("system", Message.Type.SYSTEM, "user-connect=\""+username+"\"", null), new String[]{username});
            try {
                output.writeObject(message);
            } catch (IOException ex) {
                System.out.println("error");
            }
        }
        while (!server.requestToStop) { // infinite loop input which this thread waits for incoming messages and processes them
            Message<?> msg_received;
            try {
                msg_received = (Message) input.readObject(); // read the message from the client
            } catch (ClassCastException e) {
                throw new ServerException("The object received was not correct of correct Class");
            } catch (EOFException | SocketException e) {
                // user has probably quit
                System.out.println(e.getClass().toString()+" caught in ServerConnector (" + e.getMessage() + ")");
                int port1 = this.socket.getPort();
                if (this.username.equals("")) { // not logged in yet
                    this.server.removeClientLoggingIn(port1);
                    //clients dont need to know that
                    //server.sendToClients(new Message<>("system", Message.Type.SYSTEM, "user-disconnect=\":" + port1 + "\""));
                    if (connected) {
                        server.onConnectionClosed(":" + port1, port1, "unknown");
                    }
                } else {
                    this.server.removeClient(username);
                    if (connected) { // if connection was fine until this moment (it is false if connection was closed intentionally - the one closing it will set connected to false)
                        server.sendToClients(new Message<>("system", Message.Type.SYSTEM, "user-disconnect=\"" + username + "\""));
                        server.onConnectionClosed(username, port1, "unknown");
                    }
                }
                connected = false;
                return;
            } catch (Exception ex) {
                errPrintln(ex.getMessage());
                return;
            }

            // print to server's output
            try {
                String nameToPrint = "";
                if (msg_received.getMessageType() != Message.Type.LOGIN_REQUEST && server.isLoginRequired()) {
                    nameToPrint = " [" + msg_received.getMessageSender() + "]";
                }
                if (null != msg_received.getMessageType()) {
                    switch (msg_received.getMessageType()) {
                        case DATA_STRING:
                            println("[" + this.socket.getPort() + "]" + nameToPrint + ": " + msg_received.getMessageObject()); // print the incoming message input the console
                            break;
                        case DATA:
                            println("[" + this.socket.getPort() + "]" + nameToPrint + ": " + msg_received.getMessageObject().getClass()); // print the incoming message input the console
                            break;
                        case COMMAND:
                            println("[" + this.socket.getPort() + "]" + nameToPrint + ": " + "COMMAND: " + msg_received.getMessageObject().toString()); // print the incoming message input the console
                            break;
                        default:
                            println("[" + this.socket.getPort() + "]" + nameToPrint + ": " + msg_received.getMessageType().toString()); // print the incoming message input the console
                            break; // print the incoming message input the console
                    }
                }
            } catch (IllegalArgumentException e) {
                errPrintln("[error]: You are using incorrect time format");
            }

            // what to do with the received message (depends on the type)
            if (msg_received.getMessageType() == Message.Type.LOGIN_REQUEST) {
                if (server.isLoginRequired()) {
                    // clientLoggedIn() adds client to the Map
                    String wantedUsername = msg_received.getMessageSender();
                    if (server.clientLoggedIn(wantedUsername, this.socket.getPort(), this)) {
                        username = wantedUsername;
                        Message<String> message = new Message<>("system", Message.Type.LOGIN_SUCCESSFUL, "Login was successful", new String[]{username});
                        println("[system]: User " + username + " has connected");
                        server.onSuccessfulLogin(username, this.socket.getPort());
                        server.sendToOtherClients(new Message<>("system", Message.Type.SYSTEM, "user-connect=\""+username+"\"", null), new String[]{username});
                        try {
                            output.writeObject(message);
                        } catch (IOException ex) {
                            System.out.println("error");
                        }
                    } else {
                        Message<String> message = new Message<>("system", Message.Type.LOGIN_DENIED, "Username already exists. Pick another one", new String[]{""+this.socket.getPort()});
                        println("[system]: User at " + this.socket.getPort() + " has been denied because of duplicated username");
                        try {
                            output.writeObject(message);
                        } catch (IOException ex) {
                            System.out.println("error");
                        }
                    }
                }
            } else if (msg_received.getMessageType() == Message.Type.LOGOFF) {
                logoff();
                return;
            } else if (msg_received.getMessageType() == Message.Type.DATA || msg_received.getMessageType() == Message.Type.DATA_STRING) {
                try {
                    String failedRecipients = Server.printArray(server.sendToClients(msg_received), ", "); // send message to clients
                    // return warning message to sender if some recipients did not exist
                    if (!failedRecipients.equals("") && msg_received.getReplyAllowed()) {
                        Message<String> returnWarningMsg = new Message<>("system", Message.Type.SYSTEM, "recipients-not-exist=\"" + failedRecipients + "\"", new String[]{msg_received.getMessageSender()}, false);
                        server.sendToClients(returnWarningMsg);
                    }

                } catch (Exception e) {
                    errPrintln("[system]: There was a problem while sending the message to clients");
                    e.printStackTrace(System.err);
                }
            } else if (msg_received.getMessageType() == Message.Type.COMMAND) {
                clientCommand(msg_received.getMessageObject().toString());
            }

        }
    }

    /**
     * Receives a command from client that must start with a slash /. You can use following
     * commands: /who, /logoff, /help.
     * It may return reply message of type {@link server_api.Message.Type#SYSTEM}, {@link server_api.Message.Type#DATA_STRING} or {@link server_api.Message.Type#ERROR},
     * depending on the type of command and switches.
     * 
     * @param command This is the command.
     */
    public void clientCommand(String command) {
        if (!command.startsWith("/")) {
            return;
        }
        String[] comm = command.split(" ");
        switch(comm[0]){
            case "/who":
                if(comm.length==1){
                    sendToThisClient("system", Message.Type.DATA_STRING, "Connected clients: " + Server.printSet(server.getClients(), ", "));
                }else if(comm.length == 2){
                    if(comm[1].equals("-c")){
                        System.out.println("printSet: "+ Server.printSet(server.getClients(), ","));
                        sendToThisClient("system", Message.Type.SYSTEM, "connected-clients=\"" + Server.printSet(server.getClients(), ",") + "\"");
                    }else if(comm[1].equals("-?")){
                        sendToThisClient("system", Message.Type.DATA_STRING, "/who: Shows connected clients. You can use switch:\n  -c : This returns a reply for easy computer reading. Message looks like 'connected-clients=\"client1,client2,clientN\"' where clients are written between quotation marks separated with comma");
                    }else if(comm[1].startsWith("-")){
                        sendToThisClient("system", Message.Type.ERROR, "/who: Unknown switch. Use -c or -? for help");
                    }else{
                        sendToThisClient("system", Message.Type.ERROR, "/who: Too many arguments. Use -? for help");
                    }
                }else{
                    sendToThisClient("system", Message.Type.ERROR, "/who: Too many arguments. Use -? for help");
                }
                break;
            case "/logoff":
                if(comm.length == 1){
                    logoff();
                }else if(comm.length == 2 && comm[1].equals("-?")){
                    sendToThisClient("system", Message.Type.DATA_STRING, "/logoff: Logs off the sender.");
                }else
                    sendToThisClient("system", Message.Type.ERROR, "/logoff: Too many arguments. Use -? for help");
                break;                
            case "/help":
                if(comm.length == 1){
                    println("[server]: /help: Supported commands:\n  /who : prints out connected users\n  /logoff : logs off the user\nServer can send you system messages. They are formatted like: type-of-message=\"value1,value2\"\nValues are between the quotation marks and are separated by comma. There are following types of messages:\n  user-connect : Contains a username of a single user that has connected\n  user-disconnect : Contains a username of a single user that has disconnected\n  recipients-not-exist : It is a reply message when message that was sent to specific clients that did not exist\n  connected-clients : Lists all the clients that are currently connected. It can be requested by command /who");
                }else{
                    println("[server]: /help: Command takes no arguments. If you want more information about specific command, use switch -? at that command");
                }  
            default:
                println("[server]: Unknown command " + comm[0]);
                
        }
    }
    
    private void logoff(){
        close();
        server.removeClient(this.username);
        connected = false;
        server.sendToClients(new Message<>("system", Message.Type.SYSTEM, "user-disconnect=\"" + username + "\""));
        int port1 = this.socket.getPort();
        if (server.isLoginRequired()) {
            server.onConnectionClosed(this.username, port1, "logoff");
        } else {
            server.onConnectionClosed(this.username, port1, "logoff"); // colon in front of the username means it is a port
        }
    }
    
    /**
     * Sends message to client with which this object is communicating.
     * 
     */
    private void sendToThisClient(String sender, Message.Type type, String text){
        try {
            this.output.writeObject(new Message<>(sender, type, text, new String[]{username}));
            this.output.flush();
        } catch (IOException ex){ }
    }

    /**
     * Closes {@link ObjectOutputStream}, {@link ObjectInputStream} and
     * {@link Socket}.
     *
     */
    public void close() {

        System.out.println("Closing at " + this.toString());
        connected = false;
        try {
            output.close();
        } catch (IOException ex) {
            System.out.println("Exception 37");
        }
        try {
            input.close();
        } catch (IOException ex) {
            System.out.println("Exception 38");
        }
        try {
            socket.close();
        } catch (IOException ex) {
        }
    }

    /**
     * It calls the method by the same name from Server class, which prints text
     * to Standard Output, if you wish to print elsewhere simply override the
     * method.<br>
     * You can also override {@link #print(String)} and
     * {@link #errPrintln(String)}.
     *
     * @param s This is the output text.
     * @see Server#println(String)
     */
    public void println(String s) {
        server.println(s);
    }

    /**
     * It calls the method by the same name from Server class, which prints text
     * to Standard Output, if you wish to print elsewhere simply override the
     * method.<br>
     * You can also override {@link #println(String)} and
     * {@link #errPrintln(String)}.
     *
     * @param s This is the output text.
     * @see Server#print(String)
     */
    public void print(String s) {
        server.print(s);
    }

    /**
     * It calls the method by the same name from Server class, which prints text
     * to Standard Output, if you wish to print elsewhere simply override the
     * method.<br>
     * You can also override {@link #println(String)} and
     * {@link #print(String)}.
     *
     * @param s This is the output text.
     * @see Server#errPrintln(String)
     */
    public void errPrintln(String s) {
        server.errPrintln(s);
    }

}
