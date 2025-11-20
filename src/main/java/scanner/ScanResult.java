package scanner;


import java.net.InetAddress;
import java.util.Objects;

public class ScanResult {

    // Target identification
    private final String host;
    private final InetAddress address;
    private final Integer port;


    // Result data
    private final State state;
    private final long responseTime;
    private final String reason;


    ScanResult(Builder builder) {
        this.host = Objects.requireNonNull(builder.host, "Host cannot be null");
        this.address = builder.address;
        this.port = builder.port;
        this.state = Objects.requireNonNull(builder.state, "State cannot be null");
        this.responseTime = builder.responseTime;
        this.reason = builder.reason;
    }

    // Getters
    public InetAddress getAddress() { return address; }
    public Integer getPort() { return port; }
    public State getState() { return state; }

    // Convenience methods
    public boolean isPortOpen() {
        return port != null && state == State.OPEN;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

            // Port scan format: 192.168.1.1:80 -> OPEN (12ms)
            sb.append(host)
                    .append(":")
                    .append(port)
                    .append(" -> ")
                    .append(state)
                    .append(" (")
                    .append(responseTime)
                    .append("ms)");

        if (reason != null && !reason.isEmpty()) {
            sb.append(" [").append(reason).append("]");
        }

        return sb.toString();
    }

    // Builder Pattern
    public static class Builder {
        private String host;
        private InetAddress address;
        private Integer port;
        private State state;
        private long responseTime;
        private String reason;

//        public Builder host(String host) {
//            this.host = host;
//            return this;
//        }

        public Builder address(InetAddress address) {
            this.address = address;
            this.host = address.getHostAddress(); // Auto-set host from address
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }


        public Builder state(State state) {
            this.state = state;
            return this;
        }

        public Builder responseTime(long responseTime) {
            this.responseTime = responseTime;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }



        public ScanResult build() {
            return new ScanResult(this);
        }
    }

}