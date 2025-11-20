package cli.display;

import scanner.ScanResult;
import scanner.ScanTiming;
import scanner.State;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PortScanDisplay{

    private final List<ScanResult> openPortList;
    private static final int BOX_WIDTH = 58;



    public PortScanDisplay(List<ScanResult> openPortResults){
        this.openPortList = openPortResults;
    }

    private static void printBoxLine(String content) {
        String padded = padRight(content);
        System.out.printf("| %s|%n", padded);
    }
    private static String padRight(String text) {
        if (text.length() >= BOX_WIDTH - 1) return text;
        return text + " ".repeat(BOX_WIDTH - text.length() - 1);
    }


    public void printScanInfo(List<InetAddress> targetHosts, List<Integer> portList, ScanTiming timing) {
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        System.out.println("|" + centerText("PORT SCANNER") + "|");
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        printBoxLine("hosts:  "+ targetHosts.size());
        printBoxLine("Ports:  "+ portList.size());
        printBoxLine("Timing: "+ timing.name());
        printBoxLine(" - Timeout(ms):            "+ timing.initialTimeout());
        printBoxLine(" - Threads on host Ports:  "+ timing.portThreadPoolSize());
        printBoxLine(" - Threads on hosts:       "+ timing.targetThreadPoolSize());
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        printBoxLine("Press Ctrl+C to stop and show partial results");
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        System.out.println();
    }

    public void printReport(List<ScanResult> allResults, int totalHosts, long scanDurationMs) {
        System.out.println("\n+" + "=".repeat(BOX_WIDTH) + "+");
        System.out.println("|" + centerText("SCAN RESULTS") + "|");
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+\n");

        // Print results (first 6 and last 4 if too many)
        printResultsList(openPortList);

        // Print statistics
        printStatistics(allResults, openPortList, totalHosts, scanDurationMs);
    }

    private void printResultsList(List<ScanResult> allResults) {
        if (allResults.isEmpty()) {
            System.out.println("No open ports found.\n");
            return;
        }

        final int SHOW_FIRST = 6;
        final int SHOW_LAST = 4;
        final int THRESHOLD = SHOW_FIRST + SHOW_LAST + 1;
        final int RES_SIZE = allResults.size();

        if (RES_SIZE <= THRESHOLD) {
            // Show all results
            for (int  i = 0; i < RES_SIZE; i++) {
                System.out.printf("[%d] " + allResults.get(i) +"\n", i);
            }
        } else {
            // Show first 6
            for (int i = 0; i < SHOW_FIRST; i++) {
                System.out.printf("[%d] " + allResults.get(i) +"\n", i);
            }

            // Show omitted count
            int omitted = RES_SIZE - SHOW_FIRST - SHOW_LAST;
            System.out.println("\n  ... " + omitted + " more result(s) omitted ...\n");

            // Show last 4
            for (int i = allResults.size() - SHOW_LAST; i < RES_SIZE; i++) {
                System.out.printf("[%d] " + allResults.get(i) +"\n", i);
            }
        }
        System.out.println();
    }

    private void printStatistics(List<ScanResult> allResults, List<ScanResult> openResults,
                                 int totalHosts, long scanDurationMs) {
        // Count UNIQUE hosts with at least one open port
        Set<String> uniqueHostsUp = openResults.stream()
                .map(r -> r.getAddress().getHostAddress())
                .collect(Collectors.toSet());

        // Count by state
        Map<State, Long> stateCounts = allResults.stream()
                .collect(Collectors.groupingBy(ScanResult::getState, Collectors.counting()));

        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        System.out.println("|" + centerText("STATISTICS") + "|");
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        printBoxLine("Hosts scanned:     " + totalHosts);
        printBoxLine("Hosts up:          " + uniqueHostsUp.size());
        printBoxLine("Total ports:       " + allResults.size());
        printBoxLine("Open ports:        " + openResults.size());
        printBoxLine("Closed ports:      " + stateCounts.getOrDefault(State.CLOSED, 0L));
        printBoxLine("Filtered/Timeout:  " +
                (stateCounts.getOrDefault(State.TIMEOUT, 0L) +
                        stateCounts.getOrDefault(State.FILTERED, 0L)));
        printBoxLine("Scan duration:     " + formatScanDuration(scanDurationMs));
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+\n");

        if (!openResults.isEmpty()) {
            printPortDistribution(openResults);
        }
    }

    private void printPortDistribution(List<ScanResult> openResults) {
        // Count open ports by port number
        Map<Integer, Long> portCounts = openResults.stream()
                .collect(Collectors.groupingBy(ScanResult::getPort, Collectors.counting()));

        // Find max count for percentage calculation
        long maxCount = portCounts.values().stream().mapToLong(Long::longValue).max().orElse(1);

        // Sort by count (descending)
        List<Map.Entry<Integer, Long>> sortedPorts = portCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10) // Show top 10
                .toList();

        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
        System.out.println("|" + centerText("OPEN PORT DISTRIBUTION") + "|");
        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");

        for (Map.Entry<Integer, Long> entry : sortedPorts) {
            int port = entry.getKey();
            long count = entry.getValue();
            double percentage = (count * 100.0) / maxCount;

            String bar = createBar(percentage, 20);
            String leftContent = String.format("Port %-6d %s %3d%% (%d host%s)",
                    port,
                    bar,
                    (int) percentage,
                    count,
                    count > 1 ? "s" : " ");
            printBoxLine(leftContent);
        }

        System.out.println("+" + "=".repeat(BOX_WIDTH) + "+");
    }


    private String createBar(double percentage, int maxLength) {
        int filledLength = (int) Math.round((percentage / 100.0) * maxLength);
        int emptyLength = maxLength - filledLength;

        return "#".repeat(Math.max(0, filledLength)) +
                ".".repeat(Math.max(0, emptyLength));
    }

    private String formatScanDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return String.format("%.2fs", millis / 1000.0);
        } else {
            long minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    private String centerText(String text) {
        int padding = (BOX_WIDTH - text.length()) / 2;
        int rightPadding = BOX_WIDTH - text.length() - padding;
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, rightPadding));
    }
}
