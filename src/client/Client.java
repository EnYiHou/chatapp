package client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import protocol.BooleanSerializer;
import protocol.BytesSerializer;
import protocol.Conversation;
import protocol.ConversationSerializer;
import protocol.ERequestType;
import protocol.EResponseType;
import protocol.FileAnnouncement;
import protocol.FileAnnouncementSerializer;
import protocol.IntegerSerializer;
import protocol.ListSerializer;
import protocol.Message;
import protocol.ProtocolFormatException;
import protocol.Request;
import protocol.RequestSerializer;
import protocol.Response;
import protocol.ResponseSerializer;
import protocol.SendFileAnnouncement;
import protocol.SendFileAnnouncementSerializer;
import protocol.StringSerializer;

public class Client {
    private final String host;
    private final int port;
    ClientNotificationRunnable runnable;
    Thread notificationThread;
    private final LimitedPriorityBlockingQueue<Message> messages;
    private final int maxMessages;
    private final static int DEFAULT_MAX_MESSAGES = 15;
    private final static int TRANSFER_BLOCK_SIZE = 2048;
    private AtomicBoolean newMessages;
    private String currentConversationPassword;
    
    Client(String host, int port) throws NoSuchAlgorithmException {
        this(host, port, DEFAULT_MAX_MESSAGES);
    }
    
    Client(String host, int port, int maxMessages) throws NoSuchAlgorithmException {
        this.host = host;
        this.port = port;
        this.maxMessages = maxMessages;
        this.messages = new LimitedPriorityBlockingQueue<>(
            this.maxMessages,
            new MessageTimestampComparator(),
            () -> this.newMessages.set(true)
        );
        this.newMessages = new AtomicBoolean(false);
        this.currentConversationPassword = "";
    }
    
    public int getMaxMessages() {
        return this.maxMessages;
    }
    
    private Response request(Request req, EResponseType expectedResponseType)
        throws ProtocolFormatException, IOException, ServerErrorException {
        Response resp;
        
        try (Socket sock = new Socket(this.host, this.port)) {
            byte[] serializedRequest = new RequestSerializer().serialize(req);
            
            sock.getOutputStream().write(
                    new IntegerSerializer().serialize(serializedRequest.length)
            );
            
            sock.getOutputStream().write(serializedRequest);
            
            resp = new ResponseSerializer().deserialize(
                sock.getInputStream().readNBytes(
                    new IntegerSerializer().deserialize(
                        sock.getInputStream().readNBytes(
                            IntegerSerializer.size()
                        )
                    ).getValue()
                )
            ).getValue();
            
            if (resp.getType() != expectedResponseType) {
                if (resp.getType() != EResponseType.ERROR)
                    throw new ProtocolFormatException(
                            "Got invalid response type for request"
                    );
                
                throw new ServerErrorException(
                    new StringSerializer().deserialize(
                        resp.getBody()
                    ).getValue()
                );
            }
        }
        
        return resp;
    }
    
    public void authenticate(
        String username,
        String password,
        boolean signingUp
    ) throws ServerErrorException, IOException, ProtocolFormatException {
        this.runnable = new ClientNotificationRunnable(
            this.host,
            this.port,
            username,
            password,
            signingUp,
            messages
        );
        
        this.notificationThread = new Thread(this.runnable);
        this.notificationThread.start();
    }
    
    public void logout()
        throws ProtocolFormatException, IOException, ServerErrorException {
        
        this.request(
            new Request(
                ERequestType.LOGOUT,
                this.runnable.getCookie(),
                new byte[0]
            ),
            EResponseType.EMPTY
        );
        
        this.notificationThread.interrupt();
        try {
            this.notificationThread.join();
        } catch (InterruptedException ex) {
        }
    }
    
    public Conversation createConversation(String name)
        throws ProtocolFormatException, IOException, ServerErrorException {
        Response resp = this.request(
            new Request(
                ERequestType.CREATE_CONVO,
                this.runnable.getCookie(),
                new StringSerializer().serialize(name)
            ),
            EResponseType.CONVERSATION
        );
        
        return new ConversationSerializer().deserialize(
            resp.getBody()
        ).getValue();
    }
    
    public List<Conversation> listConversations()
        throws ProtocolFormatException, IOException, ServerErrorException {
        Response resp = this.request(
            new Request(
                ERequestType.LIST_CONVO,
                this.runnable.getCookie(),
                new byte[0]
            ),
            EResponseType.CONVERSATIONS
        );

        return new ListSerializer<>(new ConversationSerializer()).deserialize(
            resp.getBody()
        ).getValue();
    }
    
    public void joinConversation(String code, String password)
        throws ProtocolFormatException, IOException, ServerErrorException {
        this.messages.clear();
        
        this.currentConversationPassword = password;
        
        this.request(
            new Request(
                ERequestType.JOIN_CONVO,
                this.runnable.getCookie(),
                new StringSerializer().serialize(code)
            ),
            EResponseType.EMPTY
        );
    }
    
    public void changePassword(String newPassword)
        throws ProtocolFormatException, IOException, ServerErrorException {
        this.request(
            new Request(
                ERequestType.CHANGE_PASSWD,
                this.runnable.getCookie(),
                new StringSerializer().serialize(newPassword)
            ),
            EResponseType.EMPTY
        );
    }
    
    public Message[] getMessagesSnapshot() throws GeneralSecurityException, IOException {
        Message[] snapshot = new Message[this.messages.size()];
        
        this.messages.toArray(snapshot);
        this.newMessages.set(false);
        
        Arrays.sort(snapshot, new MessageTimestampComparator());
        
        for (int i = 0; i < snapshot.length; ++i) {
            ByteArrayInputStream reader = new ByteArrayInputStream(
                snapshot[i].getMessage()
            );

            snapshot[i] = new Message(
                new Cryptor(
                    this.currentConversationPassword,
                    reader.readNBytes(Cryptor.SALT_LENGTH)
                ).decrypt(reader.readAllBytes()),
                snapshot[i].getAuthor(),
                snapshot[i].getTimestamp()
            );
        }
            
        return snapshot;
    }

    public void sendMessage(String input)
        throws ProtocolFormatException, IOException, ServerErrorException, GeneralSecurityException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        
        final Cryptor cryptor = new Cryptor(currentConversationPassword);

        builder.writeBytes(cryptor.getSalt());
        builder.writeBytes(cryptor.encrypt(input.getBytes()));
        
        this.request(
            new Request(
                ERequestType.SEND_MSG,
                this.runnable.getCookie(),
                new BytesSerializer().serialize(
                    builder.toByteArray()
                )
            ),
            EResponseType.EMPTY
        );
    }

    public boolean newMessages() {
        return this.newMessages.get();
    }
    
    public void receiveFile(
        int timeout,
        Consumer<String> transferCodeNotifier,
        Predicate<FileAnnouncement> verifier,
        BiConsumer<FileAnnouncement, Long> notifier
    ) throws IOException, ProtocolFormatException, ServerErrorException {
        try (Socket receiverSock = new Socket(host, port)) {
            receiverSock.setSoTimeout(timeout);
            
            byte[] serializedRequest = new RequestSerializer().serialize(
                new Request(
                    ERequestType.RECV_FILE,
                    this.runnable.getCookie(),
                    new byte[0]
                )
            );

            receiverSock.getOutputStream().write(
                new IntegerSerializer().serialize(
                    serializedRequest.length
                )
            );

            receiverSock.getOutputStream().write(serializedRequest);
            
            transferCodeNotifier.accept(
                new StringSerializer().deserialize(
                    receiverSock.getInputStream().readNBytes(
                        new IntegerSerializer().deserialize(
                            receiverSock.getInputStream().readNBytes(
                                IntegerSerializer.size()
                            )
                        ).getValue()
                    )
                ).getValue()
            );
            
            FileAnnouncement senderAnnouncement;
            boolean senderValid;
            
            do {
                try {
                    senderAnnouncement =
                        new FileAnnouncementSerializer().deserialize(
                            receiverSock.getInputStream().readNBytes(
                                new IntegerSerializer().deserialize(
                                    receiverSock.getInputStream().readNBytes(
                                        IntegerSerializer.size()
                                    )
                                ).getValue()
                            )
                        ).getValue();
                } catch (SocketTimeoutException ex) {
                    return;
                }

                senderValid = verifier.test(senderAnnouncement);

                byte[] serializedValidation = new BooleanSerializer().serialize(
                    senderValid
                );

                receiverSock.getOutputStream().write(
                    new IntegerSerializer().serialize(
                        serializedValidation.length
                    )
                );

                receiverSock.getOutputStream().write(serializedValidation);
            } while (!senderValid);
            
            String sanitizedFileName = Paths
                .get(senderAnnouncement.getFileName())
                .getFileName()
                .toString();
            
            try (
                FileOutputStream fp = new FileOutputStream(sanitizedFileName)
            ) {
                long written = 0;
                while (written < senderAnnouncement.getSize()) {
                    byte[] block = receiverSock.getInputStream().readNBytes(
                        (int)Math.min(
                            senderAnnouncement.getSize() - written,
                            TRANSFER_BLOCK_SIZE
                        )
                    );
                    
                    fp.write(block);
                    
                    written += block.length;
                }
            }
            
            Response resp = new ResponseSerializer().deserialize(
                receiverSock.getInputStream().readNBytes(
                    new IntegerSerializer().deserialize(
                        receiverSock.getInputStream().readNBytes(
                            IntegerSerializer.size()
                        )
                    ).getValue()
                )
            ).getValue();
            
            if (resp.getType() != EResponseType.EMPTY) {
                if (resp.getType() != EResponseType.ERROR)
                    throw new ProtocolFormatException(
                        "Got invalid response type for request"
                    );
                
                throw new ServerErrorException(
                    new StringSerializer().deserialize(
                        resp.getBody()
                    ).getValue()
                );
            }
        }
    }
    
    public void sendFile(String transferCode, String path)
        throws IOException, ProtocolFormatException, FileTransferException, ServerErrorException {
        final long fileSize = new File(path).length();
        
        try (
            FileInputStream fp = new FileInputStream(path);
            Socket senderSock = new Socket(this.host, this.port)
        ) {
            String sanitizedFileName = Paths
                .get(path)
                .getFileName()
                .toString();
            
            byte[] serializedRequest = new RequestSerializer().serialize(
                new Request(
                    ERequestType.SEND_FILE,
                    this.runnable.getCookie(),
                    new SendFileAnnouncementSerializer().serialize(
                        new SendFileAnnouncement(
                            transferCode,
                            sanitizedFileName,
                            fileSize
                        )
                    )
                )
            );

            senderSock.getOutputStream().write(
                new IntegerSerializer().serialize(
                    serializedRequest.length
                )
            );
            
            senderSock.getOutputStream().write(serializedRequest);
            
            try {
                senderSock.getOutputStream().write(
                    new IntegerSerializer().serialize(TRANSFER_BLOCK_SIZE)
                );
                
                while (fp.available() > 0)
                    senderSock.getOutputStream().write(
                        fp.readNBytes(TRANSFER_BLOCK_SIZE)
                    );
            } catch (IOException ex) {
                throw new FileTransferException("Transfer code rejected");
            }
            
            Response resp = new ResponseSerializer().deserialize(
                senderSock.getInputStream().readNBytes(
                    new IntegerSerializer().deserialize(
                        senderSock.getInputStream().readNBytes(
                            IntegerSerializer.size()
                        )
                    ).getValue()
                )
            ).getValue();
            
            if (resp.getType() != EResponseType.EMPTY) {
                if (resp.getType() != EResponseType.ERROR)
                    throw new ProtocolFormatException(
                        "Got invalid response type for request"
                    );
                
                throw new ServerErrorException(
                    new StringSerializer().deserialize(
                        resp.getBody()
                    ).getValue()
                );
            }
        }
    }
}
