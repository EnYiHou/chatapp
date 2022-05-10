package protocol;

public class FileAnnouncement {
    private final String sender;
    private final String fileName;
    private final long size;

    public FileAnnouncement(String sender, String fileName, long size) {
        this.sender = sender;
        this.fileName = fileName;
        this.size = size;
    }

    public String getSender() {
        return sender;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }
}
