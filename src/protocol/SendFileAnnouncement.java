package protocol;

public class SendFileAnnouncement {
    private final String transferCode;
    private final String fileName;
    private final long size;

    public SendFileAnnouncement(String transferCode, String fileName, long size) {
        this.transferCode = transferCode;
        this.fileName = fileName;
        this.size = size;
    }

    public String getTransferCode() {
        return transferCode;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }
}
