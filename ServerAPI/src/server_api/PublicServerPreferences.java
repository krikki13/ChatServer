package server_api;

import java.io.Serializable;

/**
 * This class is the super class of {@link PrivateServerPreferences} and is meant to be sent to Clients after connection
 * is established, so the Client can its preferences as it is required.
 * 
 * @author KRIKKI
 * @version 1
 * @since 23. 6. 2017
 */
public class PublicServerPreferences implements Serializable {
    /**
     * This is the port on which Server is listening. It needs to have value between 0 and 65535 inclusive.
     */
    protected int port = -1;
    /**
     * This defines if login with username is required.
     */
    protected boolean loginRequired = false;
    /**
     * This defines the minimal username length (only necessary if login is required).
     */
    protected int minUsernameLength = -1;
    /**
     * This defines the maximal username length (only necessary if login is required).
     */
    protected int maxUsernameLength = -1;
    /**
     * This defines the usernames that are forbidden. It uses regex and before
     * comparing, String will be converted to lower case, so it is recommended
     * to write forbiddenUsernames all in lower case. Usernames system and error
     * are always forbidden.<br> An example: "guest|unknown"
     */
    protected String forbiddenUsernames = "";
    /**
     * This defines kinds of usernames that are allowed. It uses regex. 
     * Signs that are never allowed are : " ' , \ / &lt; &gt; [ ] - <br>
     * <br> An example: "[a-zA-Z0-9][a-zA-Z0-9]*"
     */ // REALLY signs that are never allowed are " ' , \ / < > [ ] but because of html i had to use &lt; and &gt;
    protected String allowedUsernames = ""; 
    /**
     * Words that forbidden to be sent. It uses regex and before
     * comparing, String will be converted to lower case, so it is recommended
     * to write forbiddenWords all in lower case.<br>
     * An example: "broccoli|cauliflower"
     */
    protected String forbiddenWords = ""; 
    
    /**
     * This zero argument constructor does not set any variables. It is expected that they will be set using setter methods.
     * If they are not set, ServerException may be thrown.
     * 
     */
    protected PublicServerPreferences() {
    }

    /**
     * Creates a copy of the PublicServerPreferences object passed in as an argument.
     * 
     * @param prefs2 This is the PublicServerPreferences object you want duplicated.
     */
    public PublicServerPreferences(PublicServerPreferences prefs2) {
        if(prefs2 == null){
            return;
        }
        this.port = prefs2.getPort();
        this.loginRequired = prefs2.isLoginRequired();
        this.minUsernameLength = prefs2.getMinUsernameLength();
        this.maxUsernameLength = prefs2.getMaxUsernameLength();
        this.forbiddenUsernames = prefs2.getForbiddenUsernames();
        this.allowedUsernames = prefs2.getAllowedUsernames();
        this.forbiddenWords = prefs2.getForbiddenWords();
    }

    /**
     * Constructs the object by setting some variables at once. It is meant to have loginRequired set to false, since
     * it does not set variables about usernames (<i>int minUsernameLength, int maxUsernameLength, String forbiddenUserames, String allowedCharacters</i>).
     * If you want to set all of them consider using constructor {@link #PublicServerPreferences(int, boolean, int, int, java.lang.String, java.lang.String, java.lang.String)}
     * 
     * @param port This is the port at which the Server will be listening.
     * @param loginRequired This defines if clients will have to connect with username.
     * @param forbiddenWords This defines which words are forbidden for sending using regex.
     */
    public PublicServerPreferences(int port, boolean loginRequired, String forbiddenWords) {
        this.port = port;
        this.loginRequired = loginRequired;
        this.forbiddenWords = forbiddenWords;
    }
    
    /**
     * Constructs the object by setting all variables at once. If loginRequired will be false, values of variables 
     * <i>int minUsernameLength, int maxUsernameLength, String forbiddenUserames, String allowedCharacters</i> are irrelevant and constructor
     * {@link #PublicServerPreferences(int, boolean, String)} can be used instead.
     * 
     * @param port This is the port at which the Server will be listening.
     * @param loginRequired This defines if clients will have to connect with username.
     * @param minUsernameLength This defines the minimal length of the username if login is required. Otherwise value of this variable is irrelevant.
     * @param maxUsernameLength This defines the maximal length of the username if login is required. Otherwise value of this variable is irrelevant.
     * @param forbiddenUsernames This defines which usernames are forbidden using regex if login is required. Otherwise value of this variable is irrelevant.
     * @param allowedUsernames This defines how username can be like using regex if login is required. Otherwise value of this variable is irrelevant.
     * @param forbiddenWords This defines which words are forbidden for sending using regex.
     */
    public PublicServerPreferences(int port, boolean loginRequired, int minUsernameLength, int maxUsernameLength, String forbiddenUsernames, String allowedUsernames, String forbiddenWords) {
        this.port = port;
        this.loginRequired = loginRequired;
        this.minUsernameLength = minUsernameLength;
        this.maxUsernameLength = maxUsernameLength;
        this.forbiddenUsernames = forbiddenUsernames;
        this.allowedUsernames = allowedUsernames;
        this.forbiddenWords = forbiddenWords;
    }
    
    
    /**
     * Returns the port number on which Server will be listening.
     * If variable has not been set it will throw ServerException.
     * 
     * @return Returns the port number on which Server will be listening.
     * @throws ServerException If variable has not been set.
     */
    public int getPort() {
        if(!isPortValid()) throw new ServerException("Preference for variable 'port' has not been set or is not valid");
        return port;
    }
    
    /**
     * Returns true if all variables are valid. If loginRequired is false, values of 
     * <i>int minUsernameLength, int maxUsernameLength, String forbiddenUsernames, String allowedCharacters</i>
     * are irrelevant. However calling individual isValid methods for these variables will return boolean
     * value that defines if they are valid no matter what current value of loginRequired is.
     * 
     * @return Returns true if all variables are valid.
     */
    public boolean isValid(){
        return isPortValid() && (!isLoginRequired() || (isMinUsernameLengthValid() && isMaxUsernameLengthValid() && isForbiddenUsernamesValid() && isAllowedUsernamesValid())) && isForbiddenWordsValid();
    }
    
    /**
     * Returns true if value of Server port has been set and is between 0 and 65535 (inclusive on both sides).
     * 
     * @return Returns true if value of port has been set and is valid.
     */
    public boolean isPortValid(){
        return port >= 0 && port <= 65535;
    }

    /**
     * Sets value for port on which Server will be listening. At this moment setting invalid value will have no effect, but later on
     * ServerException may be thrown.
     * 
     * @param port This is the value for Server port.
     * @see #isPortValid() 
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * Returns true if login with username is required. However if it is not, variables regarding usernames,
     * do not need to be valid.
     * This variable is automatically set to false.
     * 
     * @return Returns true if login is required.
     */
    public boolean isLoginRequired() {
        return loginRequired;
    }

    /**
     * Sets boolean value if login by username is required when a new client connects.
     * If it is set to false, variables regarding usernames do not need to be set.
     * This variable is automatically set to false.
     * 
     * @param loginRequired This defines if login by username is required.
     */
    public void setLoginRequired(boolean loginRequired) {
        this.loginRequired = loginRequired;
    }
    
    /**
     * Returns the minimal length of the username.
     * If variable has not been set and loginRequired is true, it will throw ServerException.
     * 
     * @return Returns the minimal length of the username.
     * @throws ServerException If variable has not been set and loginRequired is true.
     */
    public int getMinUsernameLength() {
        if(!isMinUsernameLengthValid()) throw new ServerException("Preference for variable 'minUsernameLength' has not been set or is not valid");
        return minUsernameLength;
    }

     /**
     * Returns true if minimal length of username has been set and is greater or equal than 1.
     * If loginRequired is set to false, the value of this variable is not important, however this method
     * still shows it as it is on its own.
     * 
     * @return Returns true if value for minimal length of username has been set and is valid.
     */
    public boolean isMinUsernameLengthValid(){
        return (minUsernameLength >= 1);
    }
    
    /**
     * Sets value for minimal username length. If loginRequired is set to false, the value of this variable is not important.
     * However if it is true, setting invalid value will have no effect at this moment, but later on
     * ServerException may be thrown.
     * 
     * @param minUsernameLength This is the value that defines minimal length of username.
     * @see #isMinUsernameLengthValid() 
     */
    public void setMinUsernameLength(int minUsernameLength) {
        this.minUsernameLength = minUsernameLength;
    }

    /**
     * Returns true if maximal length of username has been set and is greater or equal than 1.
     * If loginRequired is set to false, the value of this variable is not important, however this method
     * still shows it as it is on its own.
     * 
     * @return Returns true if value for maximal length of username has been set and is valid.
     */
    public boolean isMaxUsernameLengthValid(){
        return (maxUsernameLength >= 1);
    }
    
    /**
     * Returns the maximal length of the username.
     * If variable has not been set and loginRequired is true, it will throw ServerException.
     * 
     * @return Returns the maximal length of the username.
     * @throws ServerException If variable has not been set and loginRequired is true.
     */
    public int getMaxUsernameLength() {
        if(!isMaxUsernameLengthValid()) throw new ServerException("Preference for variable 'maxUsernameLength' has not been set or is not valid");
        return maxUsernameLength;
    }

    /**
     * Sets value for maximal username length. If loginRequired is set to false, the value of this variable is not important.
     * However if it is true, setting invalid value will have no effect at this moment, but later on
     * ServerException may be thrown.
     * 
     * @param maxUsernameLength This is the value that defines maximal length of username.
     * @see #isMaxUsernameLengthValid() 
     */
    public void setMaxUsernameLength(int maxUsernameLength) {
        this.maxUsernameLength = maxUsernameLength;
    }

    /**
     * Returns the regex String that specifies which usernames are forbidden.
     * For example "sever|error".<br>
     * If variable has not been set and loginRequired is true, it will throw ServerException.<br>
     * An example is "server|error".
     * 
     * @return Returns the regex String that specifies forbidden usernames.
     * @throws ServerException If variable has not been set and loginRequired is true.
     */
    public String getForbiddenUsernames() {
        if(!isForbiddenUsernamesValid()) throw new ServerException("Preference for variable 'forbiddenUsernames' has not been set or is not valid");
        return forbiddenUsernames;
    }
    
    /**
     * Returns true if regex String for forbidden usernames has been set and is not an empty String.
     * If loginRequired is set to false, the value of this variable is not important, however this method
     * still shows it as it is on its own.<br>
     * An example is "server|error".
     * 
     * @return Returns true if regex String for forbidden usernames has been set and is valid.
     */
    public boolean isForbiddenUsernamesValid(){
        return (allowedUsernames != null);
    }

    /**
     * Sets regex String that defines what usernames are forbidden. 
     * If loginRequired is set to false, the value of this variable is not important.
     * However if it is true, setting invalid value will have no effect at this moment, but later on
     * ServerException may be thrown. Always forbidden are system and error.<br>
     * An example is "robert|vegi.*".
     * 
     * @param forbiddenUserames This is the regex String for forbidden usernames.
     * @see #isForbiddenUsernamesValid() 
     */
    public void setForbiddenUsernames(String forbiddenUserames) {
        this.forbiddenUsernames = forbiddenUserames;
    }
    
    /**
     * Returns true if regex String that defines what kind of usernames are allowed has been set and is not an empty String.
     * If loginRequired is set to false, the value of this variable is not important, however this method
     * still shows it as it is on its own.<br>
     * An example is "^[a-zA-Z][a-zA-Z0-9]*$".
     * 
     * @return Returns true if regex String for allowed usernames has been set and is valid.
     */
    public boolean isAllowedUsernamesValid(){
        return (allowedUsernames != null);
    }
    
    /**
     * Returns the regex String that defines what kinds of usernames are allowed.
     * If variable has not been set and loginRequired is true, it will throw ServerException.<br>
     * An example is "^[a-zA-Z][a-zA-Z0-9]*$".
     * 
     * @return Returns the regex String that defines what kinds of usernames are allowed.
     * @throws ServerException If variable has not been set and loginRequired is true.
     */
    public String getAllowedUsernames() {
        if(!isAllowedUsernamesValid()) throw new ServerException("Preference for variable 'allowedUsernames' has not been set or is not valid");
        return allowedUsernames;
    }

    /**
     * Sets regex String that defines what kinds of usernames are allowed.
     * If loginRequired is set to false, the value of this variable is not important.
     * However if it is true, setting invalid value will have no effect at this moment, but later on
     * ServerException may be thrown.<br>
     * An example is "^[a-zA-Z][a-zA-Z0-9]*$".
     * 
     * @param allowedUsernames This is the regex String that defines allowed usernames.
     * @see #isAllowedUsernamesValid() 
     */
    public void setAllowedUsernames(String allowedUsernames) {
        this.allowedUsernames = allowedUsernames;
    }
    
    /**
     * Returns the regex String that defines what kind of words are forbidden for sending.
     * If variable has not been set, it will throw ServerException.<br>
     * An example is "broccolis?|cauliflower[s]?".
     * 
     * @return Returns the regex String that defines what kinds of words are forbidden.
     * @throws ServerException If variable has not been set.
     */
    public String getForbiddenWords() {
        if(!isForbiddenWordsValid()) throw new ServerException("Preference for variable 'forbiddenwords' has not been set or is not valid");
        return forbiddenWords;
    }
    
    /**
     * Returns true if regex String that defines what kind of words are forbidden for sending.
     * An example is "broccolis?|cauliflower[s]?".
     * 
     * @return Returns true if regex String for forbidden words has been set and is valid.
     */
    public boolean isForbiddenWordsValid(){
        return (forbiddenWords!= null);
    }
    
    /**
     * Sets regex String that defines what kind of words are forbidden for sending.
     * An example is "^[a-zA-Z][a-zA-Z0-9]*$".
     * 
     * @param forbiddenWords This is the regex String that defines forbidden words.
     * @see #isForbiddenWordsValid()  
     */
    public void setForbiddenWords(String forbiddenWords) {
        this.forbiddenWords = forbiddenWords;
    }
}
