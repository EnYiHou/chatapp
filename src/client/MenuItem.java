package client;

public enum MenuItem {
    CREATE_CONVO("Create conversation"),
    JOIN_CONVO("Join conversation"),
    LIST_CONVO("List conversation"),
    CHANGE_PASSWD("Change password"),
    LOGOUT("Logout");
    
    private final String message;
    
    private MenuItem(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return this.message;
    }
}
