package core;

/**
 * shutdown handler - cancels operation on Ctrl+C
 */
public class ShutdownHandler {
    private final Cancellable operation;
    private Thread shutdownHook;

    /**
     * Create shutdown handler for a cancellable operation
     */
    public ShutdownHandler(Cancellable operation) {
        this.operation = operation;
    }

    /**
     * Register the shutdown hook
     */
    public void register() {
        shutdownHook = new Thread(() -> {
            System.err.println("\n[!] Shutting down gracefully... Please wait.");

            if (operation != null) {
                operation.cancel();
            }

            // Give threads time to finish cleanly
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.err.println("[!] Shutdown complete.");
        }, "ShutdownHook");

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Unregister the shutdown hook (call on normal completion)
     */
    public void unregister() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is already shutting down, ignore
            }
        }
    }
}