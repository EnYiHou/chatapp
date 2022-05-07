package client;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import protocol.Conversation;
import protocol.Message;
import protocol.ProtocolFormatException;


public class ClientFrontend {
    private static Scanner sc;
    
    private static void printError(Exception ex) {
        if (ServerErrorException.class.isInstance(ex))
            System.err.println(
                "error: server responded with: " + ex.getMessage()
            );
        else if (IOException.class.isInstance(ex))
            System.err.println("error: connection failed");
        else if (ProtocolFormatException.class.isInstance(ex))
            System.err.println("error: " + ex.getMessage());
        else
            System.err.println("error: Unknown error");
    }
    
    private static void immediateFlush(Object x) {
        System.out.print(x);
        System.out.flush();
    }
    
    private static Client connectClient() {
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
                printError(ex);
                
                if (!ServerErrorException.class.isInstance(ex))
                    return null;
            }
        } while (true);
        
        return client;
    }
    
    private static MenuItem promptMenu() {
        System.out.println("What do you want to do?");
        for (MenuItem i : MenuItem.values())
            System.out.printf("\t[%d]: %s%n", i.ordinal(), i.getMessage());
        
        do {
            System.out.print("Select an option: ");
            
            try {
                int option = sc.nextInt();
                MenuItem item = MenuItem.values()[option];
                sc.nextLine();
                
                return item;
            } catch (InputMismatchException | ArrayIndexOutOfBoundsException ex)
            {
                System.err.printf(
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
                } catch (ServerErrorException ex) {
                    immediateFlush(promptSupplier.apply(ex.getMessage()));
                }
                
                shallReprint = true;
            }
            
            if (shallReprint || client.newMessages()) {
                shallReprint = false;
    
                final Message[] messages = client.getMessagesSnapshot();
                
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
                        message.getMessage()
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
    
    public static void main(String[] args) {
        sc = new Scanner(System.in);

        AnsiConsole.systemInstall();

        Client client = connectClient();
        
        if (client == null)
            return;
        
        while (true) {
            immediateFlush(Ansi.ansi().eraseScreen().cursor(0, 0));

            switch (promptMenu()) {
                case CHANGE_PASSWD: {
                    
                    break;
                }
                case CREATE_CONVO: {
                    try {
                        System.out.print("Conversation name: ");
                        Conversation conv = client.createConversation(
                            sc.nextLine()
                        );
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        printError(ex);
                    }
                    
                    break;
                }
                case JOIN_CONVO: {
                    try {
                        System.out.print("Conversation code: ");
                        client.joinConversation(
                            sc.nextLine()
                        );
                        
                        startChat(client);
                    } catch (
                        ServerErrorException |
                        IOException |
                        ProtocolFormatException ex
                    ) {
                        printError(ex);
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
                        printError(ex);
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
                        printError(ex);
                    }
                    
                    return;
                }
                default:
                    break;
            }
        }
    }
}