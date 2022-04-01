package server;

public class HashInvalidLengthException extends Exception {
    public HashInvalidLengthException() {
    }

    public HashInvalidLengthException(String msg) {
        super(msg);
    }
}
