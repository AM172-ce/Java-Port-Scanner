package scanner;

public enum State{

    // Positive states (port is responsive)
    OPEN("Port is open"),

    // Negative states (port is not responsive or closed)
    CLOSED("Port is closed"),
    FILTERED("Filtered by firewall"),

    // Error/timeout states
    TIMEOUT("No response within timeout"),
    ERROR("Error during probe"),
    HOST_UNREACHABLE("Host unreachable"),
    NETWORK_UNREACHABLE("Network unreachable");

    private final String description;

    State(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
