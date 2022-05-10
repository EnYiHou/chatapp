package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;
import protocol.BooleanSerializer;
import protocol.Conversation;
import protocol.ConversationSerializer;
import protocol.CookieSerializer;
import protocol.CredentialsBody;
import protocol.CredentialsBodySerializer;
import protocol.Deserialized;
import protocol.EResponseType;
import protocol.FileAnnouncement;
import protocol.FileAnnouncementSerializer;
import protocol.IntegerSerializer;
import protocol.ListSerializer;
import protocol.Message;
import protocol.MessageSerializer;
import protocol.ProtocolFormatException;
import protocol.Request;
import protocol.RequestSerializer;
import protocol.Response;
import protocol.ResponseSerializer;
import protocol.SendFileAnnouncement;
import protocol.SendFileAnnouncementSerializer;
import protocol.StringSerializer;

public class ConnectionHandler implements Runnable {
    private final Socket conn;
    private final SessionManager sessManager;
    private final MessageManager messageManager;
    private final SecretManager secretManager;
    
    ConnectionHandler(
        Socket conn,
        SecretManager secretManager,
        SessionManager authManager,
        MessageManager messageManager
    )
        throws SocketException {
        this.conn = conn;
        this.secretManager = secretManager;
        this.sessManager = authManager;
        this.messageManager = messageManager;
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
                        UserSession sess = this.sessManager.loginCookie(
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
                        UserSession sess = this.sessManager.loginCookie(
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
                        UserSession sess = this.sessManager.loginCookie(
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

                        UserSession sess = this.sessManager.login(
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
                        
                        UserSession sess = this.sessManager.signUp(
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
                        UserSession sess = this.sessManager.loginCookie(
                            req.getCookie()
                        );
                        
                        this.sessManager.logoutSession(sess);
                        
                        response = new Response(
                            EResponseType.EMPTY,
                            new byte[0]
                        );

                        break;
                    }
                    case SEND_MSG: {
                        UserSession sess = this.sessManager.loginCookie(
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
                            this.sessManager.getSessionsForConversation(
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
                        UserSession sess = this.sessManager.loginCookie(
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
                    case RECV_FILE: {
                        this.sessManager.loginCookie(
                            req.getCookie()
                        );
                        
                        final String transferCode =
                            this.sessManager.createTransfer(
                                conn
                            );
                        
                        byte[] serializedCode =
                            new StringSerializer().serialize(
                                transferCode
                            );
                        
                        this.conn.getOutputStream().write(
                            new IntegerSerializer().serialize(
                                serializedCode.length
                            )
                        );
                        
                        this.conn.getOutputStream().write(serializedCode);
                        
                        while (!this.conn.isClosed())
                            Thread.sleep(200);
                        
                        this.sessManager.stopTransfer(transferCode);
                        
                        response = new Response(
                            EResponseType.EMPTY,
                            new byte[0]
                        );
                        
                        break;
                    }
                    case SEND_FILE: {
                        UserSession sess = this.sessManager.loginCookie(
                            req.getCookie()
                        );
                        
                        SendFileAnnouncement announcement =
                            new SendFileAnnouncementSerializer().deserialize(
                                req.getBody()
                            ).getValue();
                        
                        Socket targetConn = this.sessManager.getTransfer(
                            announcement.getTransferCode()
                        );
                        
                        if (targetConn == null)
                            throw new GenericMessageException(
                                "Invalid code"
                            );
                        
                        final byte[] serializedFileAnnouncement =
                            new FileAnnouncementSerializer().serialize(
                                new FileAnnouncement(
                                    sess.getUsername(),
                                    announcement.getFileName(),
                                    announcement.getSize()
                                )
                            );
                        
                        targetConn.getOutputStream().write(
                            new IntegerSerializer().serialize(
                                serializedFileAnnouncement.length
                            )
                        );
                        
                        targetConn.getOutputStream().write(
                            serializedFileAnnouncement
                        );
                        
                        if (
                            !new BooleanSerializer().deserialize(
                                targetConn.getInputStream().readNBytes(
                                    new IntegerSerializer().deserialize(
                                        targetConn.getInputStream().readNBytes(
                                            IntegerSerializer.size()
                                        )
                                    ).getValue()
                                )
                            ).getValue()
                        ) {
                            throw new GenericMessageException(
                                "Connection refused"
                            );
                        }
                        
                        final int blockSize =
                            new IntegerSerializer().deserialize(
                                this.conn.getInputStream().readNBytes(
                                    IntegerSerializer.size()
                                )
                            ).getValue();
                        
                        for (long i = 0; i < announcement.getSize();) {
                            byte[] block =
                                this.conn.getInputStream().readNBytes(
                                    (int)Math.min(
                                        blockSize,
                                        announcement.getSize() - i
                                    )
                                );
                            
                            targetConn.getOutputStream().write(block);
                            
                            i += block.length;
                        }
                        
                        targetConn.close();
                        
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
