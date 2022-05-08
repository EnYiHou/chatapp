package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import protocol.Conversation;
import protocol.ConversationSerializer;
import protocol.CookieSerializer;
import protocol.CredentialsBody;
import protocol.CredentialsBodySerializer;
import protocol.Deserialized;
import protocol.EResponseType;
import protocol.IntegerSerializer;
import protocol.ListSerializer;
import protocol.Message;
import protocol.MessageSerializer;
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
                                    sess.getUsername()
                                ),
                                new StringSerializer().deserialize(
                                    req.getBody()
                                ).getValue()
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
                        Integer id;

                        if (
                            (id = this.messageManager.joinConversation(
                                this.secretManager.getUserId(
                                    sess.getUsername()
                                ),
                                new StringSerializer().deserialize(
                                    req.getBody()
                                ).getValue()
                            )) == null
                        ) {
                            throw new AuthenticationFailureException(
                                "Invalid code"
                            );
                        }

                        sess.setConversation(id);
                        List<InternalMessage> internalMessages =
                            this.messageManager.getLatestMessages(id);

                        for (InternalMessage i : internalMessages)
                            sess.sendNotification(
                                new Response(
                                    EResponseType.MESSAGE,
                                    new MessageSerializer().serialize(
                                        new Message(
                                            i.getMessage(),
                                            this.secretManager.getUsername(
                                                i.getAuthorId()
                                            ),
                                            i.getTimestamp()
                                        )   
                                    )
                                )
                            );
                        
                        response = new Response(
                            EResponseType.EMPTY,
                            new byte[0]
                        );
                        
                        break;
                    }
                    case LIST_CONVO: {
                        UserSession sess = this.authManager.loginCookie(
                            req.getCookie()
                        );

                        response = new Response(
                            EResponseType.CONVERSATIONS,
                            new ListSerializer<>(
                                new ConversationSerializer()
                            ).serialize(
                                this.messageManager.listConversations(
                                    this.secretManager.getUserId(
                                        sess.getUsername()
                                    )
                                )
                            )
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
                        
                        InternalMessage mess = this.messageManager.sendMessage(
                            this.secretManager.getUserId(sess.getUsername()),
                            sess.getConversation(),
                            new StringSerializer().deserialize(
                                req.getBody()
                            ).getValue()
                        );
                        
                        List<UserSession> sessions =
                            this.authManager.getSessionsForConversation(
                                sess.getConversation()
                            );

                        for (UserSession i : sessions)
                            i.sendNotification(
                                new Response(
                                    EResponseType.MESSAGE,
                                    new MessageSerializer().serialize(
                                        new Message(
                                            mess.getMessage(),
                                            this.secretManager.getUsername(
                                                mess.getAuthorId()
                                            ),
                                            mess.getTimestamp()
                                        )   
                                    )
                                )
                            );
                        
                        response = new Response(
                            EResponseType.EMPTY,
                            new byte[0]
                        );

                        break;
                    }
                    case CHANGE_PASSWD: {
                        UserSession sess = this.authManager.loginCookie(
                            req.getCookie()
                        );
                        
                        this.secretManager.changePassword(
                            this.secretManager.getUserId(sess.getUsername()),
                            new StringSerializer().deserialize(
                                req.getBody()
                            ).getValue()
                        );
                        
                        response = new Response(
                            EResponseType.EMPTY,
                            new byte[0]
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
            } catch (AuthenticationFailureException | GenericMessageException ex) {
                response = new Response(
                    EResponseType.ERROR,
                    new StringSerializer().serialize(ex.getMessage())
                );
            } catch (Exception ex) {
                ex.printStackTrace();
                
                response = new Response(
                    EResponseType.ERROR,
                    new StringSerializer().serialize("internal error")
                );
            } finally {
                byte[] serializedResponse = new ResponseSerializer().serialize(
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
