package protocol;

public class Request {
    private final ERequestType type;
    private final Cookie cookie;
    private final byte body[];
    
    public Request(ERequestType type, Cookie cookie, byte body[]) {
        this.type = type;
        this.cookie = cookie;
        this.body = body;
    }

    public ERequestType getType() {
        return type;
    }

    public Cookie getCookie() {
        return cookie;
    }
    
    public byte[] getBody() {
        return body;
    }
}
