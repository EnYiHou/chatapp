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
import java.util.function.Function;
import java.util.regex.Pattern;

public class MessageManager {
    private Connection dbConn;
    private final static int CODE_LENGTH = 8;
    private final static char[] CODE_SET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        .toCharArray();
    private final static String SQL_ARRAY_SEPARATOR = ",";
    private final static int DEFAULT_RETRIEVE_LIMIT = 15;
    
    private static String generateCode() {
        SecureRandom rng = new SecureRandom();
        StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);
        
        for (int i = 0; i < CODE_LENGTH; ++i)
            codeBuilder.append(CODE_SET[rng.nextInt(CODE_SET.length)]);
        
        return codeBuilder.toString();
    }
    
    private static <T> String listToSQLList(List<T> list) {
        return 
            list.stream()
                .map(t -> t.toString().getBytes())
                .map(Base64.getEncoder()::encodeToString)
                .collect(
                    Collectors.joining(
                        SQL_ARRAY_SEPARATOR,
                        SQL_ARRAY_SEPARATOR,
                        SQL_ARRAY_SEPARATOR
                    )
                );
    }
    
    private static <T> List<T> SQLListToList(
        String sqlList,
        Function<String, T> transformer
    ) {
        final String sqlListInner = sqlList.substring(
            SQL_ARRAY_SEPARATOR.length(),
            sqlList.length() - SQL_ARRAY_SEPARATOR.length()
        );
        
        if (sqlListInner.length() == 0)
            return new ArrayList<>();
        
        return Arrays.stream(sqlListInner.split(SQL_ARRAY_SEPARATOR))
            .map(Base64.getDecoder()::decode)
            .map(String::new)
            .map(transformer::apply)
            .collect(Collectors.toCollection(ArrayList::new));
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
                    "timestamp INTEGER NOT NULL," +
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
    
    public Conversation createConversation(int ownerId, String name) throws SQLException, GenericMessageException {
        String code;
        
        if (!Pattern.compile("[ -~]{3,32}").matcher(name).matches())
            throw new GenericMessageException(
                "Invalid conversation name"
            );
        
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
        
        return new Conversation(code, name);
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
                        result.getString("name")
                    )
                );
        }
        
        return conversations;
    }
    
    public List<InternalMessage> getLatestMessages(
        int conversationId,
        int limit
    ) throws SQLException {
        List<InternalMessage> latestMessages = new ArrayList<>(limit);
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "SELECT timestamp, author_id, message FROM messages " +
                "WHERE conversation_id = ? " +
                "ORDER BY timestamp DESC LIMIT ?"
        )) {
            stmt.setInt(1, conversationId);
            stmt.setInt(2, limit);
            
            ResultSet result = stmt.executeQuery();
            
            while (result.next())
                latestMessages.add(
                    new InternalMessage(
                        result.getString("message"),
                        result.getInt("author_id"),
                        result.getLong("timestamp")
                    )
                );
        }
        
        return latestMessages;
    }
    
    public List<InternalMessage> getLatestMessages(int conversationId) throws SQLException {
        return this.getLatestMessages(conversationId, DEFAULT_RETRIEVE_LIMIT);
    }
    
    public Integer joinConversation(int userId, String code)
        throws SQLException {
        Integer conversationId = null, ownerId = null;
        List<Integer> members = null;

        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "SELECT id, owner_id, member_ids FROM conversations WHERE code = ?"
        )) {
            stmt.setString(1, code);
            
            ResultSet r = stmt.executeQuery();
            
            if (r.next()) {
                ownerId = r.getInt("owner_id");
                conversationId = r.getInt("id");
                members = SQLListToList(r.getString("member_ids"), Integer::valueOf);
            }
        }
        
        if (conversationId == null || members == null || ownerId == null)
            return null;
        
        if (members.contains(userId) || ownerId == userId)
            return conversationId;        
        
        members.add(userId);
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "UPDATE conversations SET member_ids = ? WHERE id = ?"
        )) {
            stmt.setString(1, listToSQLList(members));
            stmt.setInt(2, conversationId);
            
            stmt.execute();
        }
        
        return conversationId;
    }
    
    public InternalMessage sendMessage(
        int senderId,
        int conversationId,
        String content
    ) throws SQLException, GenericMessageException {
        final long timestamp = System.currentTimeMillis();
        
        if (!Pattern.compile("[ -~]{1,280}").matcher(content).matches())
            throw new GenericMessageException(
                "Invalid message"
            );
        
        try (PreparedStatement stmt = this.dbConn.prepareStatement(
            "INSERT INTO messages(" +
                "timestamp, conversation_id, author_id, message" +
            ") VALUES (?, ?, ?, ?)"
        )) {
            stmt.setLong(1, timestamp);
            stmt.setInt(2, conversationId);
            stmt.setInt(3, senderId);
            stmt.setString(4, content);

            stmt.execute();
        }
        
        return new InternalMessage(content, senderId, timestamp);
    }
}
