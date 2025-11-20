package scanner;

import java.io.IOException;
import java.net.*;

/**
 * Enhanced TCP probe with better state detection
 */
public class TcpProbe {
    private final InetAddress target;
    private final int port;
    private final int timout;

    public TcpProbe(InetAddress target, int port, int timout) {
        this.target = target;
        this.port = port;
        this.timout = timout;
    }

    public ScanResult execute() {
        long start = System.currentTimeMillis();

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target, port), timout);
            long elapsed = System.currentTimeMillis() - start;

            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.OPEN)
                    .responseTime(elapsed)
                    .reason("TCP connection established")
                    .build();

        } catch (SocketTimeoutException e) {
            long elapsed = System.currentTimeMillis() - start;
            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.TIMEOUT)
                    .responseTime(elapsed)
                    .reason("Connection timeout (possibly filtered)")
                    .build();

        } catch (ConnectException e) {
            long elapsed = System.currentTimeMillis() - start;
            // Analyze the exception message for clues
            return analyzeConnectException(e, elapsed);

        } catch (NoRouteToHostException e) {
            long elapsed = System.currentTimeMillis() - start;
            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.HOST_UNREACHABLE)
                    .responseTime(elapsed)
                    .reason("No route to host (ICMP unreachable)")
                    .build();

        } catch (PortUnreachableException e) {
            long elapsed = System.currentTimeMillis() - start;
            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.FILTERED)
                    .responseTime(elapsed)
                    .reason("Port unreachable (ICMP filtered)")
                    .build();

        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            return analyzeIOException(e, elapsed);
        }
    }

    private ScanResult analyzeConnectException(ConnectException e, long elapsed) {
        String message = e.getMessage().toLowerCase();

        // "Connection refused" = port is closed (got RST packet)
        if (message.contains("connection refused")) {
            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.CLOSED)
                    .responseTime(elapsed)
                    .reason("Connection refused (RST received)")
                    .build();
        }

        // "Connection timed out" without SocketTimeoutException
        if (message.contains("timed out")) {
            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.FILTERED)
                    .responseTime(elapsed)
                    .reason("Connection timed out (likely filtered)")
                    .build();
        }

        // "Network is unreachable"
        if (message.contains("network is unreachable")) {
            return ScanResult.builder()
                    .address(target)
                    .port(port)
                    .state(State.NETWORK_UNREACHABLE)
                    .responseTime(elapsed)
                    .reason("Network unreachable")
                    .build();
        }

        // Unknown connection error
        return ScanResult.builder()
                .address(target)
                .port(port)
                .state(State.ERROR)
                .responseTime(elapsed)
                .reason("Connection error: " + e.getMessage())
                .build();
    }

    private ScanResult analyzeIOException(IOException e, long elapsed) {
        return ScanResult.builder()
                .address(target)
                .port(port)
                .state(State.ERROR)
                .responseTime(elapsed)
                .reason(State.ERROR.getDescription() + "\nI/O error: " + e.getClass().getSimpleName())
                .build();
    }
}