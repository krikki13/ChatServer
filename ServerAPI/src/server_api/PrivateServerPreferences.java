package server_api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Scanner;


/**
 * This class extends {@link PublicServerPreferences} and defines the Preferences each {@link Server} must have. 
 * It defines how {@link Server} will behave and a ({@link PublicServerPreferences}) object will be sent to {@link Client} as
 * the first message after establishing connection, so it can set it's Preferences the same way. Before PrivateServerPreferences is passed to Server, it should
 * have all variable set correctly. Although it is allowed to create the object with zero argument constructor, all
 * variables must be set using setter methods before passing the Preferences object to Server and starting it (or ServerException is imminent).
 * Variables can also be checked using {@link #isValid} methods. 
 * 
 * @author KRIKKI
 * @version 1.0
 * @since 23. 6. 2017
 */
public class PrivateServerPreferences extends PublicServerPreferences implements Serializable {
    /**
     * This defines how many clients can be connected to Server at once. Others will receive a denial message.
     */
    private int maxNumberOfClients = -1;
    /**
     * This defines how time stamps will look like. String is used by SimpleDateFormat, so you should use something like dd.MM.yyyy HH:mm:ss
     */
    private String timeStampFormat = "";
    
    /**
     * Constructs the object, but does not set any variables. These need to be set using setter methods,
     * before passing it to {@link Server} and starting it (or ServerException is imminent).
     */
    public PrivateServerPreferences() {
    }
    
    /**
     * Creates a copy of the PrivateServerPreferences object passed in as an argument.
     * 
     * @param prefs2 This is the PrivateServerPreferences object you want duplicated.
     */
    public PrivateServerPreferences(PrivateServerPreferences prefs2) {
        super(prefs2);
        if(prefs2 == null){
            return;
        }
        this.maxNumberOfClients = prefs2.getMaxNumberOfClients();
        this.timeStampFormat = prefs2.getTimeStampFormat();
    }

    /**
     * Constructs the object by setting all variables at once. If loginRequired will be false, values of variables 
     * <i>int minUsernameLength, int maxUsernameLength, String forbiddenUsernames, String allowedCharacters</i> are irrelevant and constructor
     * {@link #PrivateServerPreferences(int, int, boolean, String)} can be used instead.
     * 
     * @param maxNumberOfClients This is the maximum amount of clients that can be connected to Server at once.
     * @param port This is the port at which the Server will be listening.
     * @param loginRequired This defines if clients will have to connect with username.
     * @param minUsernameLength This defines the minimal length of the username if login is required. Otherwise value of this variable is irrelevant.
     * @param maxUsernameLength This defines the maximal length of the username if login is required. Otherwise value of this variable is irrelevant.
     * @param forbiddenUsernames This defines which usernames are forbidden using regex if login is required. Otherwise value of this variable is irrelevant.
     * @param allowedUsernames This defines how username can be like using regex if login is required. Otherwise value of this variable is irrelevant.
     * @param forbiddenWords This defines which words are forbidden for sending using regex.
     * @param timeStampFormat This is the format in which time stamp will be displayed (like dd.MM.yyyy HH:mm:ss).
     */
    public PrivateServerPreferences(int maxNumberOfClients, int port, boolean loginRequired, int minUsernameLength, int maxUsernameLength, String forbiddenUsernames, String allowedUsernames, String forbiddenWords, String timeStampFormat) {
        super(port, loginRequired, minUsernameLength, maxUsernameLength, forbiddenUsernames, allowedUsernames, forbiddenWords);
        this.maxNumberOfClients = maxNumberOfClients;
        this.timeStampFormat = timeStampFormat;
    }

    /**
     * Constructs the object by setting some variables at once. It is meant to have loginRequired set to false, since
     * it does not set variables about usernames (<i>int minUsernameLength, int maxUsernameLength, String forbiddenUsernames, String allowedCharacters</i>).
     * If you want to set all of them consider using constructor {@link #PrivateServerPreferences(int, int, boolean, int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String)}
     * 
     * @param maxNumberOfClients This is the maximum amount of clients that can be connected to Server at once.
     * @param port This is the port at which the Server will be listening.
     * @param loginRequired This defines if clients will have to connect with username.
     * @param forbiddenWords This defines which words are forbidden for sending using regex.
     */
    public PrivateServerPreferences(int maxNumberOfClients, int port, boolean loginRequired, String forbiddenWords) {
        super(port, loginRequired, forbiddenWords);
        this.maxNumberOfClients = maxNumberOfClients;
    }
    
    /**
     * Returns maximum number of clients that can be connected at once.
     * If variable has not been set it will throw ServerException.
     * 
     * @return Returns maximum number of clients.
     * @throws ServerException If variable has not been set.
     */
    public int getMaxNumberOfClients() throws ServerException{
        if(!isMaxNumberOfClientsValid()) throw new ServerException("Preference for variable 'maxNumberOfClients' has not been set or is not valid");
        return maxNumberOfClients;
    }

    /**
     * Returns true if value has been set and is valid. It is valid if it is greater than 0.
     * 
     * @return Returns true if value has been set and is valid.
     */
    public boolean isMaxNumberOfClientsValid(){
        return (maxNumberOfClients > 0);
    }
    
    /**
     * Sets maximum number of clients that can be connected at once. It can be set to any number,
     * however it should be set to number greater than 0 to be valid and ServerException later on will be avoided.
     * 
     * @param maxNumberOfClients This is the maximum number of clients that connected at once.
     * @see #isMaxNumberOfClientsValid() 
     */
    public void setMaxNumberOfClients(int maxNumberOfClients) {
        this.maxNumberOfClients = maxNumberOfClients;
    }
    
    /**
     * Returns format for the time stamp that is shown in front of every message displayed on the Server.
     * It may be equal to "", which means no time stamp will be displayed.
     * If value is incorrect it will throw ServerException. Value should look something like dd.MM.yyyy HH:mm:ss
     * 
     * @return Returns maximum number of clients.
     * @throws ServerException If variable has not been set.
     */
    public String getTimeStampFormat() throws ServerException{
        if(!isTimeStampFormatValid()) throw new ServerException("Preference for variable 'timeStampFormat' has not been set or is not valid");
        return timeStampFormat;
    }
    
    /**
     * Returns true, if format for time stamps is correct, or if it equals "" 
     * (which means no time stamp will be displayed).
     * 
     * @return Returns true if  time stamp format is correct or equals "".
     */
    public boolean isTimeStampFormatValid(){
        if(timeStampFormat.equals("")) return true;
        try{
            new SimpleDateFormat(timeStampFormat);
            return true;
        }catch(Exception e){
            return false;
        }
    }
    
    /**
     * Sets time stamp format to the new value.
     * This method does not check for validity of the new format.
     * That can be done with {@link #isTimeStampFormatValid()}.
     * 
     * @param newFormat This is the new format for time stamps.
     */
    public void setTimeStampFormat(String newFormat){
        timeStampFormat = newFormat;
    }
    
    /**
     * Returns true if all the variables are valid. If loginRequired is false, values of variables regarding usernames
     * are irrelevant.
     * 
     * @return Returns true if all the variables are valid.
     */
    @Override
    public boolean isValid(){
        return super.isValid() && isMaxNumberOfClientsValid() && isTimeStampFormatValid();
    }

    /**
     * Reads from text file specified as an argument. This method does not require specific file extension, but it will throw ServerException
     * if file is incorrectly formatted or an unknown variable is found. If not all variables were defined in text file they will be skipped, 
     * but they will need to be filled in before passing the object to Server and starting it. You can check if they are valid using {@link #isValid()}
     * or individual isValid methods.
     * 
     * @param fileName This is the name of the text file you want read.
     * @return Returns a PrivateServerPreferences object that has been defined in a file.
     * @throws FileNotFoundException If file has not been found.
     * @throws ServerException If file is incorrectly formatted or an unknown variable is found. 
     * @see #writeToTextFile(server_api.PrivateServerPreferences, java.lang.String) 
     * @see #readFromBinaryFile(java.lang.String) 
     */
    public static PrivateServerPreferences readFromTextFile(String fileName) throws FileNotFoundException, ServerException{
        String var = "";
        String value = "";
        PrivateServerPreferences prefs = new PrivateServerPreferences();
        try(Scanner scan = new Scanner(new File(fileName))){
            while(scan.hasNext()){
                String vrstica = scan.nextLine();
                if(vrstica.contains("=")){
                    var = vrstica.substring(0, vrstica.indexOf("=")).toLowerCase().trim();
                    value = vrstica.substring(vrstica.indexOf("=")+1).trim();
                    if(value.matches("\".*\"")) 
                        value = value.substring(1, value.length()-1);
                    switch(var){
                        case "port": 
                            prefs.setPort(Integer.parseInt(value));
                            break;
                        case "loginrequired": 
                            if(value.equals("true") || value.equals("1"))
                                prefs.setLoginRequired(true);
                            else if(value.equals("false") || value.equals("0"))
                                prefs.setLoginRequired(false);
                            else
                                throw new ServerException("Variable loginRequired has irregular value ("+value+")");
                            break;
                        case "minusernamelength": 
                            prefs.setMinUsernameLength(Integer.parseInt(value));
                            break;
                        case "maxusernamelength": 
                            prefs.setMaxUsernameLength(Integer.parseInt(value));
                            break;     
                        case "forbiddenusernames": 
                            prefs.setForbiddenUsernames(value);
                            break;  
                        case "allowedusernames": 
                            prefs.setAllowedUsernames(value);
                            break; 
                        case "forbiddenwords":
                            prefs.setForbiddenWords(value);
                            break;
                        case "maxnumberofclients":
                            prefs.setMaxNumberOfClients(Integer.parseInt(value));
                            break;
                        case "timestampformat":
                            prefs.setTimeStampFormat(value);
                            break;
                        default:
                            throw new ServerException("Unrecognized variable in: '"+vrstica+"'");
                    }
                }
            }
        }catch(NumberFormatException e){
            throw new ServerException("Variable "+var+" does not have an integer value ("+value+")");
        }
        return prefs;
    }
    
    /**
     * It writes the given PrivateServerPreferences object to file specified as String argument. It will overwrite over possible existing file.
     * The file extension is not important. It will only write variables that are valid.
     * 
     * @param prefs This is the PrivateServerPreferences object you want written in a text file.
     * @param fileName This is the name of the file.
     * @throws FileNotFoundException If it cannot write to file.
     * @see #readFromTextFile(java.lang.String) 
     * @see #writeToBinaryFile(server_api.PrivateServerPreferences, java.lang.String) 
     */
    public static void writeToTextFile(PrivateServerPreferences prefs, String fileName) throws FileNotFoundException{
        try(PrintWriter writer = new PrintWriter(new File(fileName))){
            if(prefs.isPortValid())
                writer.println("port = \"" + prefs.getPort() +"\"");
            writer.println("loginRequired = " + prefs.isLoginRequired());
            if(prefs.isMinUsernameLengthValid())
                writer.println("minUsernameLength = \"" + prefs.getMinUsernameLength() +"\"");
            if(prefs.isMaxUsernameLengthValid())
                writer.println("maxUsernameLength = \"" + prefs.getMaxUsernameLength() +"\"");
            if(prefs.isForbiddenUsernamesValid())
                writer.println("forbiddenUsernames = \"" + prefs.getForbiddenUsernames() +"\"");
            if(prefs.isAllowedUsernamesValid())
                writer.println("allowedUsernames = \"" + prefs.getAllowedUsernames() +"\"");
            if(prefs.isForbiddenWordsValid())
                writer.println("forbiddenWords = \"" + prefs.getForbiddenWords() +"\"");
            if(prefs.isMaxNumberOfClientsValid())
                writer.println("maxNumberOfClients = \"" + prefs.getMaxNumberOfClients() +"\"");
            if(prefs.isTimeStampFormatValid())
                writer.println("timestampformat = \"" + prefs.getTimeStampFormat() +"\"");
            System.out.println("timestampformat: "+prefs.timeStampFormat);
        }
    }
    
    /**
     * Reads from binary file specified as an argument. This method does not require specific file extension, but it will throw ServerException
     * if file is incorrectly formatted. If not all variables were defined in text file they will be skipped, 
     * but they will need to be filled in before passing the object to Server and starting it. You can check if they are valid using {@link #isValid()}
     * or individual isValid methods.
     * 
     * 
     * @param fileName This is the name of the file.
     * @return Returns the PrivateServerPreferences object.
     * @throws ServerException If file is not correctly formatted.
     * @throws IOException If there has been an error while reading.
     * @see #readFromTextFile(java.lang.String) 
     * @see #writeToBinaryFile(server_api.PrivateServerPreferences, java.lang.String) 
     */
    public static PrivateServerPreferences readFromBinaryFile(String fileName) throws ServerException, IOException{
        try(ObjectInputStream output = new ObjectInputStream(new FileInputStream(new File(fileName)))){
            if(output.readShort() != 1240){
                throw new ServerException("The file you entered is not in the correct format for private server preferences");
            }
            return (PrivateServerPreferences) output.readObject();
        } catch (ClassNotFoundException|ClassCastException ex) {
            throw new ServerException("The file you entered is not in the correct format for private server preferences");
        }
    }
    
    /**
     * It writes the given PrivateServerPreferences object to file specified as String argument. It will overwrite over possible existing file.
     * The file extension is not important. It will only write variables that are valid.
     * 
     * @param prefs This is the PrivateServerPreferences object you want written in a text file.
     * @param fileName This is the name of the file.
     * @throws FileNotFoundException If it cannot write to file.
     * @see #readFromBinaryFile(java.lang.String) 
     * @see #writeToTextFile(server_api.PrivateServerPreferences, java.lang.String) 
     */
    public static void writeToBinaryFile(PrivateServerPreferences prefs, String fileName) throws FileNotFoundException, IOException{
        try(ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(new File(fileName)))){
            output.writeShort(1240);
            output.writeObject(prefs);
        }
    }
    
    /**
     * Returns an object of type {@link PublicServerPreferences}, which is the super class of PrivateServerPreferences.
     * 
     * @return Returns an object of type {@link PublicServerPreferences}.
     */
    public PublicServerPreferences forPublic(){
        return (PublicServerPreferences) this;
    }
  
}
