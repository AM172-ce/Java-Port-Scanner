package cli.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**<pre>
 * Parse and expand multiple target specifications into individual IPs
 * Supports:
 * - Single IPs: 192.168.1.1
 * - Range in octets: 192.168.1.1-10, 192.168-170.1.1
 * - Comma-separated ranges: 192.168.1.1-5,7-12 (skips 6)
 * - CIDR notation: 192.168.1.0/24
 * - Hostnames: example.com
 * </pre>
 */
public class IpRangeParser {

    private static final int MAX_CIDR_HOSTS = 65536;

    /**
     * Parse targets and return unique InetAddress objects (recommended)
     *
     * @param rawTargets List of target specifications
     * @return List of unique InetAddress objects (order preserved)
     */
    public static List<InetAddress> parseTargets(List<String> rawTargets) {
        return parseTargets(rawTargets, true);
    }

    /**
     * Parse targets with option to keep or remove duplicates
     *
     * @param rawTargets List of target specifications
     * @param removeDuplicates If true, return only unique addresses
     * @return List of InetAddress objects
     */
    public static List<InetAddress> parseTargets(List<String> rawTargets, boolean removeDuplicates) {
        if (rawTargets == null || rawTargets.isEmpty()) {
            return Collections.emptyList();
        }

        if (removeDuplicates) {
            return parseTargetsUnique(rawTargets);
        } else {
            return parseTargetsAll(rawTargets);
        }
    }

    /**
     * Internal: Parse and return unique addresses
     */
    private static List<InetAddress> parseTargetsUnique(List<String> rawTargets) {
        Set<InetAddress> uniqueAddresses = new LinkedHashSet<>();

        for (String target : rawTargets) {
            if (target == null || target.isBlank()) {
                continue;
            }
            List<String> expandedIps = expandTarget(target.trim());

            for (String ip : expandedIps) {
                try {
                    uniqueAddresses.add(InetAddress.getByName(ip));
                } catch (UnknownHostException e) {
                    System.err.printf("Warning: Could not resolve '%s': %s%n", ip, e.getMessage());
                }
            }
        }

        return new ArrayList<>(uniqueAddresses);
    }

    /**
     * Internal: Parse and return all addresses (including duplicates)
     */
    private static List<InetAddress> parseTargetsAll(List<String> rawTargets) {
        List<InetAddress> addresses = new ArrayList<>();

        for (String target : rawTargets) {
            if (target == null || target.isBlank()) {
                continue;
            }

            List<String> expandedIps = expandTarget(target.trim());

            for (String ip : expandedIps) {
                try {
                    addresses.add(InetAddress.getByName(ip));
                } catch (UnknownHostException e) {
                    System.err.printf("Warning: Could not resolve '%s': %s%n", ip, e.getMessage());
                }
            }
        }

        return addresses;
    }

    private static List<String> expandTarget(String target) {
        if (target.contains("/")) {
            return expandCidr(target);
        }

        if (target.contains(".")) {
            return expandIpRange(target);
        }

        return Collections.singletonList(target);
    }

    private static List<String> expandIpRange(String range) {
        String[] octets = range.split("\\.", -1);
        if (octets.length != 4) {
            return Collections.singletonList(range);
        }

        try {
            List<Set<Integer>> octetSets = new ArrayList<>(4);
            for (String octet : octets) {
                octetSets.add(parseOctetRange(octet));
            }

            return generateIps(octetSets);

        } catch (IllegalArgumentException e) {
            return Collections.singletonList(range);
        }
    }

    /**<pre>
     * Parse an octet that may contain:
     * - Single number: "1"
     * - Range: "1-5"
     * - Multiple ranges: "1-5,7-12" (comma-separated)
     * </pre>
     */
    private static Set<Integer> parseOctetRange(String octet) {
        Set<Integer> values = new LinkedHashSet<>();

        for (String part : octet.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (part.contains("-")) {
                String[] bounds = part.split("-", 2);
                int start = Integer.parseInt(bounds[0].trim());
                int end = Integer.parseInt(bounds[1].trim());

                validateOctet(start);
                validateOctet(end);

                if (start > end) {
                    throw new IllegalArgumentException("Invalid range: " + start + "-" + end);
                }

                for (int i = start; i <= end; i++) {
                    values.add(i);
                }
            } else {
                int val = Integer.parseInt(part);
                validateOctet(val);
                values.add(val);
            }
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("Empty octet range");
        }

        return values;
    }

    private static void validateOctet(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Octet out of range [0-255]: " + value);
        }
    }

    private static List<String> generateIps(List<Set<Integer>> octetSets) {
        int capacity = octetSets.stream()
                .mapToInt(Set::size)
                .reduce(1, (a, b) -> a * b);

        List<String> result = new ArrayList<>(capacity);

        Integer[][] octets = new Integer[4][];
        for (int i = 0; i < 4; i++) {
            octets[i] = octetSets.get(i).toArray(Integer[]::new);
        }

        StringBuilder sb = new StringBuilder(15);

        for (int o1 : octets[0]) {
            for (int o2 : octets[1]) {
                for (int o3 : octets[2]) {
                    for (int o4 : octets[3]) {
                        sb.setLength(0);
                        sb.append(o1).append('.')
                                .append(o2).append('.')
                                .append(o3).append('.')
                                .append(o4);
                        result.add(sb.toString());
                    }
                }
            }
        }

        return result;
    }

    private static List<String> expandCidr(String cidr) {
        try {
            String[] parts = cidr.split("/", 2);
            if (parts.length != 2) {
                return Collections.singletonList(cidr);
            }

            String baseIp = parts[0].trim();
            int prefix = Integer.parseInt(parts[1].trim());

            if (prefix < 0 || prefix > 32) {
                System.err.println("Warning: Invalid CIDR prefix: " + prefix);
                return Collections.singletonList(cidr);
            }

            long ip = ipToLong(baseIp);
            long mask = (prefix == 0) ? 0 : (0xFFFF_FFFFL << (32 - prefix)) & 0xFFFF_FFFFL;
            long network = ip & mask;
            long broadcast = network | (~mask & 0xFFFF_FFFFL);

            long start = network;
            long end = broadcast;

            if (prefix < 31) {
                start++;
                end--;
            }

            long size = end - start + 1;
            if (size > MAX_CIDR_HOSTS) {
                System.err.printf("Warning: CIDR too large (%d hosts), limiting to %d%n",
                        size, MAX_CIDR_HOSTS);
                end = start + MAX_CIDR_HOSTS - 1;
            }

            List<String> result = new ArrayList<>((int) (end - start + 1));
            for (long i = start; i <= end; i++) {
                result.add(longToIp(i));
            }

            return result;

        } catch (IllegalArgumentException e) {
            System.err.println("Warning: Invalid CIDR: " + cidr + " - " + e.getMessage());
            return Collections.singletonList(cidr);
        }
    }

    private static long ipToLong(String ip) {
        String[] octets = ip.split("\\.", -1);
        if (octets.length != 4) {
            throw new IllegalArgumentException("Invalid IP format: " + ip);
        }

        long result = 0;
        for (int i = 0; i < 4; i++) {
            int octet = Integer.parseInt(octets[i].trim());
            validateOctet(octet);
            result = (result << 8) | octet;
        }
        return result;
    }

    private static String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF);
    }
}