package protocol;

import java.util.Arrays;

public class ProtocolFormatException extends Exception {
    public ProtocolFormatException() {
    }

    public ProtocolFormatException(String msg) {
        super(msg);
    }
    
    public ProtocolFormatException(Class type, byte[] expected) {
        super(String.format("Expected %s while parsing %s", Arrays.toString(expected), type.getName()));
    }
}
