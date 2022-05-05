package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import protocol.Conversation;
import protocol.ConversationSerializer;
import protocol.CookieSerializer;
import protocol.CredentialsBody;
import protocol.CredentialsBodySerializer;
import protocol.Deserialized;
import protocol.EResponseType;
import protocol.IntegerSerializer;
import protocol.ProtocolFormatException;
import protocol.Request;
import protocol.RequestSerializer;
import protocol.Response;
import protocol.ResponseSerializer;
import protocol.StringSerializer;

public class ConnectionHandler implements Runnable {
    private final Socket conn;
    private final AuthenticationManager authManager;
    private final MessageManager messageManager;
    private final SecretManager secretManager;
    
    ConnectionHandler(
        Socket conn,
        SecretManager secretManager,
        AuthenticationManager authManager,
        MessageManager messageManager
    )
        throws SocketException {
        this.conn = conn;
        this.secretManager = secretManager;
        this.authManager = authManager;
        this.messageManager = messageManager;
        this.conn.setSoTimeout(10000);
    }
    
    @Override
    public void run() {
        Response response = null;     
        boolean shallClose = true;
        
        try {
            try {
                response = new Response(EResponseType.ERROR, new StringSerializer().serialize("unimplemented"));

                InputStream stream = this.conn.getInputStream();
                IntegerSerializer serializer = new IntegerSerializer();
                Deserialized<Integer> announcedSize = serializer.deserialize(
                    stream.readNBytes(IntegerSerializer.size())
                );

                Request req = new RequestSerializer().deserialize(
                    stream.readNBytes(announcedSize.getValue())
                ).getValue();

                switch (req.getType()) {
                    case CREATE_CONVO: {
                        UserSession sess = this.authManager.loginCookie(
                            req.getCookie()
                        );
                        
                        Conversation conv =
                            this.messageManager.createConversation(
                                this.secretManager.getUserId(
                                    sess.getUser().getUsername()
                                ),
                                new ConversationSerializer().deserialize(
                                    req.getBody()
                                ).getValue().getName()
                            );

                        response = new Response(
                            EResponseType.CONVERSATION,
                            new ConversationSerializer().serialize(conv)
                        );
                        
                        break;
                    }
                    case JOIN_CONVO: {
                        UserSession sess = this.authManager.loginCookie(
                            req.getCookie()
                        );

                        break;
                    }
                    case LOGIN: {
                        CredentialsBody loginReq =
                            new CredentialsBodySerializer()
                                .deserialize(req.getBody()).getValue();

                        UserSession sess = this.authManager.login(
                            loginReq.getUsername(),
                            loginReq.getPassword(),
                            this.conn
                        );

                        shallClose = false;
                        response = new Response(
                            EResponseType.COOKIE,
                            new CookieSerializer().serialize(sess.getCookie())
                        );

                        break;
                    }
                    case SIGNUP: {
                        CredentialsBody loginReq =
                            new CredentialsBodySerializer()
                                .deserialize(req.getBody()).getValue();
                        
                        UserSession sess = this.authManager.signUp(
                            loginReq.getUsername(),
                            loginReq.getPassword(),
                            this.conn
                        );

                        shallClose = false;
                        response = new Response(
                            EResponseType.COOKIE,
                            new CookieSerializer().serialize(sess.getCookie())
                        );

                        break;
                    }
                    case LOGOUT: {
                        UserSession sess = this.authManager.loginCookie(
                            req.getCookie()
                        );
                        
                        this.authManager.logoutSession(sess);
                        
                        response = new Response(
                            EResponseType.EMPTY,
                            new byte[0]
                        );

                        break;
                    }
                    case SEND_MSG: {
                        UserSession sess = this.authManager.loginCookie(
                            req.getCookie()
                        );

                        break;
                    }
                    default:
                        response = new Response(
                            EResponseType.ERROR,
                            new StringSerializer().serialize(
                                "unsupported operation"
                            )
                        );
                }
            } catch (SocketTimeoutException ex) {
                response = new Response(
                    EResponseType.ERROR,
                    new StringSerializer().serialize("timeout")
                );
            } catch (ProtocolFormatException ex) {
                response = new Response(
                    EResponseType.ERROR,
                    new StringSerializer().serialize(
                        "invalid message: " + ex.getMessage()
                    )
                );
            } catch (AuthenticationFailureException ex) {
                response = new Response(
                    EResponseType.ERROR,
                    new StringSerializer().serialize(ex.getMessage())
                );    
            } catch (IOException | NoSuchAlgorithmException | SQLException ex) {
                response = new Response(
                    EResponseType.ERROR,
                    new StringSerializer().serialize("internal error")
                );
            } finally {
                byte[] serializedResponse =  new ResponseSerializer().serialize(
                    response
                );
                
                this.conn.getOutputStream().write(
                    new IntegerSerializer().serialize(serializedResponse.length)
                );
                
                this.conn.getOutputStream().write(serializedResponse);

                if (shallClose)
                    this.conn.close();
            }
        } catch (IOException | ProtocolFormatException ex) {
            System.err.println(ex);
        }
    }
}
