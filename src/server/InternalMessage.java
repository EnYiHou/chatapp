package server;

public class InternalMessage {
    private final String message;
    private final int authorId;
    private final long timestamp;
    
    public InternalMessage(String message, int authorId, long timestamp) {
        this.message = message;
        this.authorId = authorId;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public int getAuthorId() {
        return authorId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
