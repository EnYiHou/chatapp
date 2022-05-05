package server;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SecretManager {
    private final Connection dbConn;
    
    public SecretManager(Connection dbConn)
        throws SecretFormatException, SQLException {
        this.dbConn = dbConn;
        
        try (Statement stmt = this.dbConn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS secrets (" +
                    "id INTEGER PRIMARY KEY," +
                    "username TEXT UNIQUE NOT NULL," +
                    "hash TEXT NOT NULL" +
                ")"
            );
        }
    }
    
    public boolean verify(String username, String password)
        throws NoSuchAlgorithmException, SQLException {
        boolean result;
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "SELECT * FROM secrets WHERE username = ? AND hash = ?"
        )) {
            stmt.setString(1, username);
            stmt.setString(2, new Hash(password).hex());
            result = stmt.executeQuery().next();
        }
        
        return result;
    }
    
    public boolean create(String username, String password)
        throws NoSuchAlgorithmException, SQLException {
        boolean result;

        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "INSERT INTO secrets(username, hash) VALUES(?, ?)"
        )) {
            stmt.setString(1, username);
            stmt.setString(2, new Hash(password).hex());
            
            try {
                stmt.execute();
                result = true;
            } catch (SQLException ex) {
                result = false;
            }
        }
        
        return result;
    }
    
    public int getUserId(String username) throws SQLException {
        int id;
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "SELECT id FROM secrets WHERE username = ?"
        )) {
            stmt.setString(1, username);
            
            ResultSet r = stmt.executeQuery();
            
            id = r.getInt("id");
        }
        
        return id;
    }
}
