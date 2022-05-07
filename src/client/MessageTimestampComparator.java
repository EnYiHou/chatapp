package client;

import java.util.Comparator;
import protocol.Message;

public class MessageTimestampComparator implements Comparator<Message> {
    @Override
    public int compare(Message o1, Message o2) {
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
}
