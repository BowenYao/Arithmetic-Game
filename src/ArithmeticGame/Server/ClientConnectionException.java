package ArithmeticGame.Server;

public class ClientConnectionException extends Exception {
    //ClientConnectionException thrown when an unexpected error happens with a specific client
    //TODO: This exception can probably be expanded once I have a better understanding of what kind of errors can occur
    public ClientConnectionException(){
        super();
    }
    public ClientConnectionException(String msg){
        super(msg);
    }

}
