package scanner;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory for better debugging and monitoring
 */
public class DaemonThreadFactory implements ThreadFactory {
    private final AtomicInteger threadIndex = new AtomicInteger(1);
    private final String threadNamePrefix;

    public DaemonThreadFactory(String poolName) {
        this.threadNamePrefix = poolName + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, threadNamePrefix + threadIndex.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}