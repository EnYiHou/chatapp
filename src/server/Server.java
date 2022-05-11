package server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Server {
    private static final String DEFAULT_DB_PATH = "chatapp.db";
    public static final int PORT = 5200;
    private final SessionManager sessManager;
    private final Connection dbConn;
    private ServerListenerThread listener;
    private final MessageManager messageManager;
    private SecretManager secretManager;
    
    public Server() throws IOException,
            HashInvalidLengthException,
            NoSuchAlgorithmException, SQLException {
        this(DEFAULT_DB_PATH);
    }
    
    public Server(String dbPath)
        throws
            HashInvalidLengthException, IOException, NoSuchAlgorithmException,
            SQLException {
        this.dbConn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        this.secretManager = new SecretManager(this.dbConn);
        this.sessManager = new SessionManager(this.secretManager);
        this.messageManager = new MessageManager(this.dbConn);
        this.listener = null;
    }
    
    public void listen() throws IOException {
        if (this.listener != null && this.listener.isAlive())
            throw new IOException("Server already listening");
        
        this.listener = new ServerListenerThread(
            PORT, this.secretManager, this.sessManager, this.messageManager
        );
        
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
