package server;

import java.sql.*;

public class DatabaseManager {
    private static final String DEFAULT_DB_PATH = "chatapp.db";
    private Connection dbConn;
    
    public DatabaseManager() throws DatabaseException {
        this(DEFAULT_DB_PATH);
    }
    
    public DatabaseManager(String path) throws DatabaseException {
        try {
            this.dbConn = DriverManager.getConnection("jdbc:sqlite:" + path);
            
            
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage());
        }
    }
    
    public void close() throws DatabaseException {
        try {
            this.dbConn.close();
        } catch (SQLException ex) {
            throw new DatabaseException(ex.getMessage());
        }
    }
}
