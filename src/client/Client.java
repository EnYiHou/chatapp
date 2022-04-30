package client;

import java.io.IOException;
import protocol.ProtocolFormatException;

public class Client {
    private final String host;
    private final int port;
    ClientNotificationRunnable runnable;
    Thread notificationThread;
        
    Client(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void authenticate(
        String username,
        String password,
        boolean signingUp
    ) throws ServerErrorException, IOException, ProtocolFormatException {
        this.runnable = new ClientNotificationRunnable(
            this.host,
            this.port,
            username,
            password,
            signingUp
        );
        
        this.notificationThread = new Thread(this.runnable);
    }
}
