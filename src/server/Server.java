package server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Server {
    private static final String DEFAULT_DB_PATH = "chatapp.db";
    public static final int PORT = 5200;
    private final AuthenticationManager authManager;
    private final Connection dbConn;
    private ServerListenerThread listener;
    
    public Server() throws IOException, SecretFormatException,
            SecretDuplicateException,HashInvalidLengthException,
            NoSuchAlgorithmException, SQLException {
        this(DEFAULT_DB_PATH);
    }
    
    public Server(String dbPath)
        throws SecretFormatException, SecretDuplicateException,
            HashInvalidLengthException, IOException, NoSuchAlgorithmException,
            SQLException {
        this.dbConn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        this.authManager = new AuthenticationManager(this.dbConn);
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
