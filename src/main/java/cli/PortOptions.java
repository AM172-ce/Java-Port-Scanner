package cli;

import picocli.CommandLine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PortOptions {

    @CommandLine.Option(
            names = {"-p", "--ports"},
            description = "Ports (e.g. 22,80,1000-1010, or 1-5,7-12)"
    )
    @SuppressWarnings("unused")
    private String ports;

    @CommandLine.Option(
            names = {"--top-ports"},
            description = "top most common ports"
    )
    @SuppressWarnings("unused")
    private boolean topPorts;


    /**
     * Parse the port specification into a list of integers
     */
    public List<Integer> getParsedPorts() {
        if (ports == null || ports.isEmpty()) {
            if(!topPorts){
                return null;
            }
            return getTopPorts();
        }
        return parsePorts(ports);
    }

    private List<Integer> parsePorts(String input) throws IllegalArgumentException{
        Set<Integer> portList = new HashSet<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-", 2);
                try{

                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());

                    if(start < 1 || end > 65535 || start > end){
                        throw new IllegalArgumentException("Invalid port range: " + part);
                    }

                    for (int i = start; i <= end; i++) {
                        portList.add(i);
                    }
                } catch(NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port range format: "
                    + part);
                }
            } else{
                try{

                    int port = Integer.parseInt(part);
                    if(port < 1 || port > 65535){
                        throw new IllegalArgumentException("Invalid port: " + port);
                    }
                    portList.add(port);
                } catch(NumberFormatException e){
                    throw new IllegalArgumentException("Invalid port range format: "
                            + part);
                }
            }
        }
        return new ArrayList<>(portList);
    }


    /**<pre>
     * Get default ports if none specified
     * Top ports for host discovery
     * </pre>
     */
    private List<Integer> getTopPorts() {
        List<Integer> defaultPorts = new ArrayList<>();
        // Top ports: HTTP, HTTPS, SSH, FTP, Telnet, SMTP, DNS, etc.
        int[] top = {21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 5432, 8080};
        for (int port : top) {
            defaultPorts.add(port);
        }
        return defaultPorts;
    }
}