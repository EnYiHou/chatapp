package server;

public class AuthenticationFailureException extends Exception {
    public AuthenticationFailureException() {
    }

    public AuthenticationFailureException(String msg) {
        super(msg);
    }
}
