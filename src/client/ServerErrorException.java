package client;

public class ServerErrorException extends Exception {
    public ServerErrorException() {
    }

    public ServerErrorException(String msg) {
        super(msg);
    }
}
