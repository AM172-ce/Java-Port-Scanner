package scanner;

import core.Cancellable;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scans multiple ports on a single target concurrently (SRP)
 */
public class HostPortScanner extends Cancellable {
    private static final Logger LOGGER = Logger.getLogger(HostPortScanner.class.getName());

    private final ScanTiming timing;
    private final ResultCollector resultCollector;

    public HostPortScanner(ScanTiming timing) {
        this(timing, new ConcurrentResultCollector());
    }
    public HostPortScanner(ScanTiming timing, ResultCollector resultCollector){
        this.timing = timing;
        this.resultCollector = Objects.requireNonNull(resultCollector, "ResultCollector cannot be null");
    }

    @Override
    protected void onCancel() {
        LOGGER.fine("SingleTargetScanner cancelled");
    }

    /**
     * Scan multiple ports on a single target
     */
    public List<ScanResult> scanPorts(InetAddress target, List<Integer> ports) {
        Objects.requireNonNull(target, "Target cannot be null");
        Objects.requireNonNull(ports, "Ports cannot be null");

        if (ports.isEmpty()) {
            return new ArrayList<>();
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                timing.portThreadPoolSize(),
                new DaemonThreadFactory("SingleTargetScanner-" + target.getHostAddress())
        );

        try {
            return executePortScans(executor, target, ports);
        } finally {
            shutdownExecutor(executor, target);
        }
    }

    private List<ScanResult> executePortScans(ExecutorService executor,
                                              InetAddress target,
                                              List<Integer> ports) {
        List<Future<ScanResult>> futures = new ArrayList<>(ports.size());


        for (int port : ports) {
            if (isCancelled()) {
                LOGGER.info("Port scan cancelled - stopping port submission for " + target);
                break;
            }
            TcpProbe probe = new TcpProbe(target, port, timing.initialTimeout());
            Future<ScanResult> future = executor.submit(probe::execute);
            futures.add(future);
        }

        // Collect results
        for (Future<ScanResult> future : futures) {
            if (isCancelled()) {
                future.cancel(true);
                continue;
            }
            try {
                resultCollector.addResult(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancel();
                LOGGER.log(Level.WARNING, "Port scan interrupted", e);
                break;
            } catch (ExecutionException e) {
                LOGGER.log(Level.SEVERE, "Error executing probe", e.getCause());
            } catch (CancellationException e) {
                LOGGER.log(Level.FINE, "Port probe was cancelled");
            }
        }
        return resultCollector.getResults();
    }

    private void shutdownExecutor(ExecutorService executor, InetAddress target) {
        if (executor == null || executor.isShutdown()) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                List<Runnable> pendingTasks = executor.shutdownNow();
                if (!pendingTasks.isEmpty()) {
                    LOGGER.warning("Port executor for " + target + " did not terminate gracefully. " +
                            pendingTasks.size() + " tasks were cancelled.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}