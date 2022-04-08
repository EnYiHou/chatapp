package client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;



public class ClientFrontend {
    private enum MenuItem {
        EXIT
    }
    
    private static MenuItem queryMenu() {
        Scanner sc = new Scanner(System.in);
        MenuItem selectedOption;
        HashMap<MenuItem, String> inputs = new HashMap<>(Map.of(
            MenuItem.EXIT, "Exit"
        ));
        
        do {
            System.out.println("Choose an option below:");
            inputs.forEach((k, v) ->
                System.out.printf("\t[%d] %s%n", k.ordinal(), v)
            );
            
            System.out.print("option: ");
            
            try {
                selectedOption = MenuItem.values()[sc.nextInt()];
                break;
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("error: invalid option");
                sc.nextLine();
            }
        } while (true);
        
        return selectedOption;
    }
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String host;
        int port;
        Client client;
        
        System.out.print("Enter server hostname: ");
        host = sc.nextLine();
        
        do {
            String input;
            
            System.out.print("Enter server port (nothing for default): ");
            input = sc.nextLine();
            
            try {
                port = Integer.parseInt(input);
                
                if (port <= 0 || port > 65535)
                    throw new NumberFormatException();
                
                break;
            } catch (NumberFormatException e) {
                System.err.println("error: invalid port number");
            }
        } while (true);
        
        try {
            client = new Client(host, port);
        } catch (IOException e) {
            System.err.println("error: could not setup client: " + e.getMessage());
            return;
        }
        
        queryMenu();
    }
}