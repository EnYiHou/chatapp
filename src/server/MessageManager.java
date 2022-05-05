package server;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import protocol.Conversation;


public class MessageManager {
    private Connection dbConn;
    private final static int CODE_LENGTH = 8;
    private final static char[] CODE_SET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        .toCharArray();
    
    private static String generateCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        
        for (int i = 0; i < CODE_LENGTH; ++i)
            codeBuilder.append(CODE_SET[rng.nextInt(CODE_SET.length)]);
        
        return codeBuilder.toString();
    }
    
    public MessageManager(Connection dbConn) throws SQLException {
        this.dbConn = dbConn;
        
        try (Statement stmt = this.dbConn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY," +
                    "conversation_id INTEGER NOT NULL," +
                    "author_id INTEGER NOT NULL," +
                    "message TEXT NOT NULL" +
                ")"
            );
            
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS conversations (" +
                    "id INTEGER PRIMARY KEY," +
                    "code TEXT UNIQUE NOT NULL," +
                    "name TEXT NOT NULL," +
                    "owner_id INTEGER NOT NULL," +
                    "member_ids STRING NOT NULL" +
                ")"
            );
        }
    }
    
    public Conversation createConversation(int ownerId, String name) throws SQLException {
        String code;
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "INSERT INTO conversations(code, name, owner_id, member_ids) " +
            "VALUES (?, ?, ?, ?)"
        )) {
            stmt.setString(2, name);
            stmt.setInt(3, ownerId);
            stmt.setString(4, "");

            while (true) {
                stmt.setString(1, (code = generateCode()));
                
                try {
                    stmt.executeUpdate();
                    break;
                } catch (SQLException ex) {
                }
            }
        }
        
        return new Conversation(code, name, List.of());
    }
}
