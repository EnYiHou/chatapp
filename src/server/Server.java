package server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Server {
    public static final int PORT = 5200;
    private final AuthenticationManager authManager;
    private final DatabaseManager dbManager;
    private ServerListenerThread listener;
    
    public Server() throws IOException, SecretFormatException,
            SecretDuplicateException,HashInvalidLengthException,
            NoSuchAlgorithmException, DatabaseException {
        this.dbManager = new DatabaseManager();
        this.authManager = new AuthenticationManager(this.dbManager);
        this.listener = null;
    }
    
    public Server(String db)
        throws SecretFormatException, SecretDuplicateException,
            HashInvalidLengthException, IOException, NoSuchAlgorithmException,
            DatabaseException {
        this.dbManager = new DatabaseManager(db);
        this.authManager = new AuthenticationManager(this.dbManager);
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
    
    public void close() throws IOException, InterruptedException, DatabaseException {
        if (this.listener == null || !this.listener.isAlive())
            throw new IOException("Server not listening");
        
        this.listener.interrupt();
        this.listener.join();
        
        this.dbManager.close();
    }
}
