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
 * Scans multiple targets concurrently
 */
public class PortScanner extends Cancellable{
    private static final Logger LOGGER = Logger.getLogger(PortScanner.class.getName());

    private final ScanTiming timing;
    private final ResultCollector resultCollector;
    private volatile ExecutorService executor;

    public PortScanner(ScanTiming timing) {
        this(timing, new ConcurrentResultCollector());
    }

    public PortScanner(ScanTiming timing,
                       ResultCollector resultCollector) {
        this.timing = Objects.requireNonNull(timing, "Configuration cannot be null");
        this.resultCollector = Objects.requireNonNull(resultCollector, "ResultCollector cannot be null");
    }

    /**
     * Cancel the ongoing scan
     */
    @Override
    protected void onCancel() {
        LOGGER.info("Scanner cancellation requested");
        shutdownExecutor();
    }



    public List<ScanResult> scan(List<InetAddress> targets, List<Integer> ports) {
        Objects.requireNonNull(targets, "Targets cannot be null");
        Objects.requireNonNull(ports, "Ports cannot be null");

        if (targets.isEmpty() || ports.isEmpty()) {
            return new ArrayList<>();
        }

        executor = Executors.newFixedThreadPool(
                timing.targetThreadPoolSize(),
                new DaemonThreadFactory("TargetScanner")
        );

        try {
            return executeScan(executor, targets, ports);
        } finally {
            shutdownExecutor();
        }
    }

    private List<ScanResult> executeScan(ExecutorService executor,
                                                List<InetAddress> targets,
                                                List<Integer> ports) {
        List<Future<List<ScanResult>>> futures = new ArrayList<>(targets.size());

        for (InetAddress target : targets) {
            if (isCancelled()) {
                System.err.println("Scan cancelled - stopping target submission");
                break;
            }
            try {
                HostPortScanner onTargetScn = new HostPortScanner(timing);
                Future<List<ScanResult>> future = executor.submit(() ->
                        onTargetScn.scanPorts(target, ports)
                );
                futures.add(future);
            } catch (RejectedExecutionException e) {
                LOGGER.warning("Task rejected for target " + target + " - executor may be shutting down");
                break;
            }
        }


        for (Future<List<ScanResult>> future : futures) {
            if (isCancelled()) {
                future.cancel(true);
                continue;
            }
            try {
                List<ScanResult> results = future.get();
                resultCollector.addResults(results);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancel();
                LOGGER.log(Level.WARNING, "target scan interrupted", e);
                break;
            } catch (ExecutionException e) {
                LOGGER.log(Level.SEVERE, "Error executing target scan", e.getCause());
            } catch (CancellationException e) {
                LOGGER.log(Level.INFO, "Target scan was cancelled");
            }
        }

        shutdownExecutor();
        return resultCollector.getResults();
    }


    private void shutdownExecutor() {
        if(executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timing.shutdownTimeoutMinutes(), TimeUnit.MINUTES)) {
                executor.shutdownNow();
                LOGGER.warning("Executor did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}