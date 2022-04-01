package server;

public class SecretDuplicateException extends Exception {
    public SecretDuplicateException() {
    }

    public SecretDuplicateException(String msg) {
        super(msg);
    }
}
