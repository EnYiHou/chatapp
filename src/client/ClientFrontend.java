package client;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import javax.crypto.NoSuchPaddingException;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import protocol.Conversation;
import protocol.Message;
import protocol.ProtocolFormatException;


public class ClientFrontend {
    private static Scanner sc;
    
    private static String formatException(Exception ex) {
        if (ServerErrorException.class.isInstance(ex))
            return String.format(
                "error: server responded with: " + ex.getMessage()
            );
        else if (
            IOException.class.isInstance(ex) ||
            ProtocolFormatException.class.isInstance(ex) ||
            FileTransferException.class.isInstance(ex)
        )
            return String.format("error: %s", ex.getMessage());
        else
            return "error: Unknown error";
    }
    
    private static void immediateFlush(Object x) {
        System.out.print(x);
        System.out.flush();
    }
    
    private static Client connectClient() throws NoSuchAlgorithmException, NoSuchPaddingException {
        String host;
        int port;
        Client client;
        
        immediateFlush(Ansi.ansi().eraseScreen().cursor(0, 0));

        System.out.print("Enter server hostname: ");
        host = sc.nextLine();
        
        do {
            String input;
            
            System.out.print("Enter server port (nothing for default): ");
            input = sc.nextLine();
            
            if (input.isEmpty()) {
                port = 5200;
                break;
            }
            
            try {
                port = Integer.parseInt(input);
                
                if (port <= 0 || port > 65535)
                    throw new NumberFormatException();
                
                break;
            } catch (NumberFormatException e) {
                System.err.println("error: invalid port number");
            }
        } while (true);
        
        client = new Client(host, port);
        
        do {
            try {
                String username;
                boolean signingUp;
                
                do {
                    String input;
                    
                    System.out.print("Do you want to sign up? (y/n): ");
                    
                    input = sc.nextLine();
                    if (
                        !"y".equalsIgnoreCase(input) &&
                        !"n".equalsIgnoreCase(input)
                    ) {
                        System.err.println("You must enter either y or n");
                        continue;
                    }
                    
                    signingUp = "y".equalsIgnoreCase(input);
                    break;
                } while (true);
                
                System.out.print("Enter username: ");
                username = sc.nextLine();
                
                System.out.print("Enter password: ");
                
                client.authenticate(
                    username,
                    sc.nextLine(),
                    signingUp
                );
                
                break;
            } catch (
                ServerErrorException |
                IOException |
                ProtocolFormatException ex
            ) {
                System.out.println(
                    Ansi.ansi()
                        .fgBrightRed()
                        .a(formatException(ex))
                        .fgDefault()
                );
                
                if (!ServerErrorException.class.isInstance(ex))
                    return null;
            }
        } while (true);
        
        return client;
    }
    
    private static MenuItem promptMenu(String lastError) {
        immediateFlush(Ansi.ansi().eraseScreen().cursor(0, 0));

        System.out.println("ChatApp");
        for (MenuItem i : MenuItem.values())
            System.out.printf("\t[%d]: %s%n", i.ordinal(), i.getMessage());
        
        immediateFlush(Ansi.ansi().saveCursorPosition());
        
        do {
            immediateFlush(
                Ansi.ansi()
                    .restoreCursorPosition()
                    .eraseScreen(Ansi.Erase.FORWARD)
                    .newline()
                    .fgBrightRed()
                    .a(lastError)
                    .fgDefault()
                    .newline()
                    .a("Select an option: ")
            );
            
            try {
                int option = sc.nextInt();
                MenuItem item = MenuItem.values()[option];
                sc.nextLine();
                
                return item;
            } catch (InputMismatchException | ArrayIndexOutOfBoundsException ex)
            {
                lastError = String.format(
                    "You must enter a value between 0 and %d%n",
                    MenuItem.values().length - 1
                );
            }
            
            sc.nextLine();
        } while (true);
    }
    
    private static void startChat(Client client)
        throws ProtocolFormatException, IOException {
        boolean shallReprint = true;
        Function<String, Ansi> promptSupplier = (lastServerError) -> Ansi.ansi()
            .eraseScreen()
            .cursor(client.getMaxMessages() + 2, 0)
            .fgBrightRed()
            .a(lastServerError)
            .fgDefault()
            .newline()
            .a("Type /exit to exit")
            .newline()
            .a("Message: ");
        
        immediateFlush(promptSupplier.apply(""));
        
        do {
            if (System.in.available() > 0) {
                String input = sc.nextLine();
                
                if ("/exit".equals(input))
                    break;
                
                try {
                    client.sendMessage(input);
                    immediateFlush(promptSupplier.apply(""));
                } catch (ServerErrorException | GeneralSecurityException ex) {
                    immediateFlush(promptSupplier.apply(ex.getMessage()));
                }
                
                shallReprint = true;
            }
            
            if (shallReprint || client.newMessages()) {
                shallReprint = false;
    
                Message[] messages;
                
                try {
                    messages = client.getMessagesSnapshot();
                } catch (GeneralSecurityException ex) {
                    messages = new Message[0];
                    
                    immediateFlush(promptSupplier.apply(ex.getMessage()));
                }
                
                immediateFlush(
                    Ansi.ansi()
                        .saveCursorPosition()
                        .cursor(
                            client.getMaxMessages() + 1,
                            0
                        )
                        .eraseScreen(Ansi.Erase.BACKWARD)
                        .cursor(
                            client.getMaxMessages() - messages.length,
                            0
                        )
                );

                for (Message message : messages)
                    System.out.printf(
                        "[%s] %s: %s%n",
                        DateFormat.getDateTimeInstance().format(
                            new Date(message.getTimestamp())
                        ),
                        message.getAuthor(),
                        new String(message.getMessage())
                    );
                
                immediateFlush(
                    Ansi.ansi().restoreCursorPosition()
                );
            } else {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                }
            }
        } while (true);
    }
    
    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException {
        sc = new Scanner(System.in);

        AnsiConsole.systemInstall();

        Client client = connectClient();
        
        String lastError = "";
        
        if (client == null)
            return;
        
        while (true) {
            MenuItem selection = promptMenu(lastError);
            
            lastError = "";
            
            switch (selection) {
                case CHANGE_PASSWD: {
                    try {
                        System.out.print("New password: ");
                        client.changePassword(
                            sc.nextLine()
                        );
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    break;
                }
                case CREATE_CONVO: {
                    try {
                        System.out.print("Conversation name: ");
                        
                        client.createConversation(
                            sc.nextLine()
                        );
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    break;
                }
                case JOIN_CONVO: {
                    try {
                        System.out.print("Conversation code: ");
                        final String conversationCode = sc.nextLine();
                        System.out.print(
                            "Conversation password: "
                        );
                        
                        client.joinConversation(
                            conversationCode, sc.nextLine()
                        );
                        
                        startChat(client);
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    break;
                }
                case LIST_CONVO: {
                    try {
                        List<Conversation> conv = client.listConversations();
                        
                        System.out.println("Conversations:");
                        
                        conv.forEach(
                            (c) -> System.out.printf(
                                "  %s (code: %s)%n",
                                c.getName(),
                                c.getCode()
                            )
                        );
                        
                        System.out.print("Press enter to continue: ");
                        sc.nextLine();
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    break;
                }
                case LOGOUT: {
                    try {
                        client.logout();
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    return;
                }
                case RECV_FILE: {
                    try {
                        immediateFlush(Ansi.ansi().eraseScreen());
                        
                        client.receiveFile(
                            0,
                            (s) -> immediateFlush(
                                Ansi.ansi()
                                    .cursor(0, 0)
                                    .a("Transfer code: ")
                                    .a(s)
                            ),
                            (announcement) -> {
                                immediateFlush(
                                    Ansi.ansi()
                                        .cursor(2, 0)
                                        .a("User ")
                                        .a(announcement.getSender())
                                        .a(" is trying to send you file '")
                                        .a(announcement.getFileName())
                                        .a("' (size: ")
                                        .a(announcement.getSize())
                                        .a(" bytes)")
                                );
                                
                                final Ansi prompt = Ansi.ansi()
                                    .cursor(3, 0)
                                    .eraseScreen(Ansi.Erase.FORWARD)
                                    .a("Accept this connection? [y/n] ");
                                
                                while (true) {
                                    immediateFlush(prompt);
                                    final String input = sc.nextLine();
                                    
                                    if ("y".equalsIgnoreCase(input))
                                        return true;
                                    else if ("n".equalsIgnoreCase(input))
                                        return false;
                                }
                            },
                            (announcement, written) -> {
                                final float progress  =
                                    (float)written / announcement.getSize();
                                
                                immediateFlush(
                                    Ansi.ansi()
                                        .cursor(3, 0)
                                        .eraseScreen(Ansi.Erase.FORWARD)
                                        .format(
                                            "Transfer: [%20s] (%.02f%%)",
                                            "=".repeat(
                                                Math.round(progress * 20)
                                            ),
                                            progress
                                        )
                                );
                            } 
                        );
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    break;
                }
                case SEND_FILE: {
                    try {
                        immediateFlush(Ansi.ansi().eraseScreen().cursor(0, 0));
                        
                        System.out.print("Transfer code: ");
                        final String transferCode = sc.nextLine();
                        
                        System.out.print("Path to file: ");
                        client.sendFile(transferCode, sc.nextLine());
                    } catch (
                        FileTransferException |
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        lastError = formatException(ex);
                    }
                    
                    break;
                }
                default:
                    break;
            }
        }
    }
}