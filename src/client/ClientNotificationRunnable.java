package client;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import protocol.Cookie;
import protocol.CookieSerializer;
import protocol.CredentialsBody;
import protocol.CredentialsBodySerializer;
import protocol.ERequestType;
import protocol.IntegerSerializer;
import protocol.ProtocolFormatException;
import protocol.Request;
import protocol.RequestSerializer;
import protocol.Response;
import protocol.ResponseSerializer;
import protocol.StringSerializer;

public class ClientNotificationRunnable implements Runnable {
    private final Socket sock;
    private final AtomicReference<Exception> cachedException;
    private final Cookie cookie;
    
    public ClientNotificationRunnable(
        String host,
        int port,
        String username,
        String password,
        boolean signingUp
    ) throws IOException, ProtocolFormatException, ServerErrorException {
        this.sock = new Socket(host, port);
        this.cachedException = new AtomicReference<>();
        
        byte[] credRequest = new RequestSerializer().serialize(
            new Request(
                signingUp ? ERequestType.SIGNUP : ERequestType.LOGIN,
                Cookie.emptyCookie(),
                new CredentialsBodySerializer().serialize(
                    new CredentialsBody(username, password)
                )
            )
        );
        
        this.sock.getOutputStream().write(
            new IntegerSerializer().serialize(credRequest.length)
        );
        
        this.sock.getOutputStream().write(credRequest);
        
        Response resp = new ResponseSerializer().deserialize(
            this.sock.getInputStream().readNBytes(
                new IntegerSerializer().deserialize(
                    this.sock.getInputStream().readNBytes(
                        IntegerSerializer.size()
                    )
                ).getValue()
            )
        ).getValue();
        
        switch (resp.getType()) {
            case COOKIE:
                this.cookie = new CookieSerializer().deserialize(
                    resp.getBody()
                ).getValue();
                break;
            case ERROR:
                throw new ServerErrorException(
                    new StringSerializer().deserialize(
                        resp.getBody()
                    ).getValue()
                );
            default:
                throw new ProtocolFormatException(
                    "Bad response type for request"
                );
        }
    }
    
    @Override
    public void run() {
        while (true) {
            if (Thread.interrupted()) {
                try {
                    this.sock.close();
                } catch (IOException ex1) {
                }
                
                return;
            }

            if (this.cachedException.get() == null)
                break;
            
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        
        try {
            Response resp = new ResponseSerializer().deserialize(
                    this.sock.getInputStream().readNBytes(
                            new IntegerSerializer().deserialize(
                                    this.sock.getInputStream().readNBytes(
                                            IntegerSerializer.size()
                                    )
                            ).getValue()
                    )
            ).getValue();
            
            switch (resp.getType()) {
                case COOKIE:
                    throw new ProtocolFormatException(
                        "Attempt to reset cookie"
                    );
                case ERROR:
                    throw new ServerErrorException(
                            new StringSerializer().deserialize(
                                    resp.getBody()
                            ).getValue()
                    );
                default:
                    throw new ProtocolFormatException(
                        "Bad response type for request"
                        );
            }
        } catch (ServerErrorException | IOException | ProtocolFormatException ex) {
            this.cachedException.set(ex);
        }
    }
    
    public void consumeException() throws Exception {
        Exception ex = this.cachedException.getAndSet(null);
        
        if (ex != null)
            throw ex;
    }
    
    public Cookie getCookie() {
        return this.cookie;
    }
}
