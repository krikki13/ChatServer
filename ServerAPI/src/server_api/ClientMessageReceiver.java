package server_api;

import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

/**
 * Is used by the Client class and is used as a message receiver.
 * 
 * @author KRIKKI
 * @version 1
 * @since 23. 6. 2017
 */
public class ClientMessageReceiver implements Runnable{

    private final ObjectInputStream in;
    private final Client client;
    

    /**
     * Sets up all necessary things to start receiving to messages.
     * 
     * @param client This is the {@link Client} class.
     * @param in This is the {@link java.io.ObjectInputStream} through which messages will be arriving.
     */
    public ClientMessageReceiver(Client client, ObjectInputStream in) {
        this.client = client;
        this.in = in;
    }

    /**
     * Starts receiving messages.
     * 
     * @throws ServerException This is thrown if an Object other than of type {@link Message} arrives.
     */
    @Override
    public void run() throws ServerException{
        Message message;
        try {
            while ((message = (Message) this.in.readObject()) != null) {                 
                if(null != message.getMessageType()){
                    // read new message
                    switch (message.getMessageType()) {
                        case PREFERENCES:
                            client.setPreferences((PublicServerPreferences)message.getMessageObject());
                            break;
                        case SYSTEM:
                            try{
                                String msg = message.getMessageObject().toString();
                                int pos = msg.indexOf("=");
                                if(pos == -1) break;
                                // the regex does trim() and then removes " if there are any... for example from >""abc   "  < you get >"abc   <
                                client.systemMessageReceived(msg.substring(0,pos), msg.substring(pos+1).replaceAll("^ *\\\"?|\\\"? *$", "")); 
                            }catch(Exception e){} 
                            break;
                        case DATA_STRING:
                        case DATA:
                            println("["+message.getMessageSender()+"]: "+message.getMessageObject().toString());
                            break;
                        case ERROR:
                            errPrintln("["+message.getMessageSender()+"]: "+message.getMessageObject().toString());
                            break;
                        case LOGIN_SUCCESSFUL:
                        case LOGIN_DENIED:
                            client.loginReplyReceived(message);
                            break;
                        default:
                            break;
                    }
                    
                }
            }
        }catch(IllegalArgumentException e){
            errPrintln("[error]: You are using incorrect time format");
        }catch(ClassCastException | StreamCorruptedException e){
            //e.printStackTrace();
        }catch (Exception e) { // usually EOFException
           client.connectionClosed("Connection with server has been lost");
        }
    }
    
    /**
     * It calls the method by the same name from Client class, which prints text to Standard Output, if you wish to print elsewhere simply override the method.<br>
     * You can also override {@link #print(String)} and {@link #errPrintln(String)}.
     * 
     * @param s This is the output text.
     * @see Client#println(String)
     */
    public void println(String s){
        client.println(s);
    }
    
    /**
     * It calls the method by the same name from Client class, which prints text to Standard Output, if you wish to print elsewhere simply override the method.<br>
     * You can also override {@link #println(String)} and {@link #errPrintln(String)}.
     * 
     * @param s This is the output text.
     * @see Client#print(String)
     */
    public void print(String s){
        client.print(s);
    }
    
    /**
     * It calls the method by the same name from Client class, which prints text to Standard Output, if you wish to print elsewhere simply override the method.<br>
     * You can also override {@link #println(String)} and {@link #print(String)}.
     * 
     * @param s This is the output text.
     * @see Client#errPrintln(String)
     */
    public void errPrintln(String s){
        client.println(s);
    }
}
