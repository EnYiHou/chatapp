package server;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import protocol.Conversation;
import java.sql.ResultSet;
import java.util.ArrayList;


public class MessageManager {
    private Connection dbConn;
    private final static int CODE_LENGTH = 8;
    private final static char[] CODE_SET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        .toCharArray();
    private final static String SQL_ARRAY_SEPARATOR = "|";
    
    private static String generateCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        
        for (int i = 0; i < CODE_LENGTH; ++i)
            codeBuilder.append(CODE_SET[rng.nextInt(CODE_SET.length)]);
        
        return codeBuilder.toString();
    }
    
    private static <T> String listToSQLList(List<T> list) {
        return list.stream()
            .map(t -> t.toString().getBytes())
            .map(Base64.getEncoder()::encodeToString)
            .map(s -> SQL_ARRAY_SEPARATOR + s + SQL_ARRAY_SEPARATOR)
            .collect(Collectors.joining(","));
    }
    
    private static <T> List<T> SQLListToList(String sqlList) {
        return Arrays.stream(sqlList.split(","))
            .map(s -> s.substring(1, s.length() - 1))
            .map(Base64.getDecoder()::decode)
            .map(t -> new String(t))
            .map(t -> (T)t)
            .toList();
    }

    private static <T> String generateSQLListFilter(T item) {
        return
            "%" +
            SQL_ARRAY_SEPARATOR +
            Base64.getEncoder().encodeToString(item.toString().getBytes()) +
            SQL_ARRAY_SEPARATOR +
            "%";
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
            stmt.setString(4, listToSQLList(List.of()));

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
    
    public List<Conversation> listConversations(int userId) throws SQLException {
        List<Conversation> conversations = new ArrayList<>();
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "SELECT code, name FROM conversations WHERE " + 
                "owner_id = ? OR " +
                "member_ids LIKE ?"
        )) {
            stmt.setInt(1, userId);
            stmt.setString(2, generateSQLListFilter(userId));
            
            ResultSet result = stmt.executeQuery();
            
            while (result.next())
                conversations.add(
                    new Conversation(
                        result.getString("code"),
                        result.getString("name"),
                        List.of()
                    )
                );
        }
        
        return conversations;
    }
}
