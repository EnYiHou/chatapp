package server;

public class InternalMessage {
    private final byte[] message;
    private final int authorId;
    private final long timestamp;
    
    public InternalMessage(byte[] message, int authorId, long timestamp) {
        this.message = message;
        this.authorId = authorId;
        this.timestamp = timestamp;
    }

    public byte[] getMessage() {
        return message;
    }

    public int getAuthorId() {
        return authorId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
