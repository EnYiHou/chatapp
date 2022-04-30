package server;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Server {
    private AuthenticationManager authManager;
    public static final int PORT = 5200;
    private ServerListenerThread listener;

    public Server() throws IOException {
        this.authManager = new AuthenticationManager();
        this.listener = null;
    }
    
    public Server(File db)
        throws SecretFormatException, SecretDuplicateException,
            HashInvalidLengthException, IOException, NoSuchAlgorithmException {
        this.authManager = new AuthenticationManager(db);
        this.listener = null;
    }
    
    public void listen() throws IOException {
        if (this.listener != null && this.listener.isAlive())
            throw new IOException("Server already listening");
        
        this.listener = new ServerListenerThread(PORT, this.authManager);
        this.listener.start();
    }
    
    public boolean isListening() {
        return this.listener.isAlive();
    }
    
    public void close() throws IOException, InterruptedException {
        if (this.listener == null || !this.listener.isAlive())
            throw new IOException("Server not listening");
        
        this.listener.interrupt();
        this.listener.join();
    }
}
