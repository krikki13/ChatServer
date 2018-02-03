package server_api;

/**
 * This exception is used by the Server API objects.
 * 
 * @author KRIKKI
 * @version 1
 * @since 23. 6. 2017
 */
public class ServerException extends RuntimeException{
    /**
     * Constructs a ServerException without error detail message.
     */
    public ServerException(){
    }

    /**
     * Constructs a ServerException with the specified detail message.
     * 
     * @param message This is the message describing the error.
     */
    public ServerException(String message) {
        super(message);
    }
}
