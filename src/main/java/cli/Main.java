package cli;

import cli.display.PortScanDisplay;
import cli.util.IpRangeParser;
import core.ShutdownHandler;
import picocli.CommandLine;
import scanner.PortScanner;
import scanner.ScanResult;
import scanner.ScanTiming;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "scannerj",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "Simple port scanner"
)
public class Main implements Callable<Integer> {


    @CommandLine.Parameters(
            arity = "0..*",
            paramLabel = "TARGET",
            description = "Target(s) - IP, hostname, range, or CIDR"
    )
    private List<String> targets = new ArrayList<>();

    @CommandLine.Mixin
    private PortOptions portOptions = new PortOptions();

    @CommandLine.Mixin
    private TimingOptions timingOptions = new TimingOptions();

    private final List<ScanResult> openPortResults = new ArrayList<>();

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        if (targets == null || targets.isEmpty()) {
            System.err.println("Error: No targets specified");
            System.err.println("Usage: scannerj [OPTIONS] <target(s)>");
            System.err.println("\nUse --help for more information");
            return 1;
        }

        // Get configuration
        ScanTiming timingPolicy = timingOptions.getConfig();
        if (timingPolicy == null) {
            System.err.println(timingOptions.errorForIncorrectInput());
            return 1;
        }

        List<InetAddress> expandedTargets = IpRangeParser.parseTargets(targets);
        List<Integer> ports = portOptions.getParsedPorts();

        PortScanner portScanner = new PortScanner(timingPolicy);

        // Setup shutdown handler
        ShutdownHandler SDownHandler = new ShutdownHandler(portScanner);
        SDownHandler.register();


        PortScanDisplay display = new PortScanDisplay(openPortResults);
        // Print scan info
        display.printScanInfo(expandedTargets, ports, timingPolicy);

        // Start scan
        long scanStart = System.currentTimeMillis();
        List<ScanResult> results = portScanner.scan(expandedTargets, ports);
        long scanDuration = System.currentTimeMillis() - scanStart;

        // Unregister shutdown handler (normal exit)
        SDownHandler.unregister();

        // Check if interrupted
        if (portScanner.isCancelled()) {
            System.err.println("\n!!! Scan was interrupted");
            return 130;
        }

        // Collect open port results
        for (ScanResult res : results) {
            if (res.isPortOpen()) {
                openPortResults.add(res);
            }
        }

        // Display results
        display.printReport(results, expandedTargets.size(), scanDuration);

        return 0;
    }

}