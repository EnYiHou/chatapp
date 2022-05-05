package client;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;
import protocol.ProtocolFormatException;


public class ClientFrontend {
    private static Scanner sc;
    
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }
    
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
    
    private static Client connectClient() {
        String host;
        int port;
        Client client;
        
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
    
    
    public static void main(String[] args) {
        sc = new Scanner(System.in);

        clearScreen();

        Client client = connectClient();
        
        if (client == null)
            return;
        
        while (true) {
            clearScreen();
            switch (promptMenu()) {
                case CHANGE_PASSWD: {
                    
                    break;
                }
                case CREATE_CONVO: {
                    
                    break;
                }
                case JOIN_CONVO: {
                    break;
                }
                case LIST_CONVO: {
                    
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