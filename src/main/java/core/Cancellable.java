package core;

import java.util.concurrent.atomic.AtomicBoolean;

/**<pre>
 * Base class for cancellable operations
 * Provides common cancellation logic,
 * subclasses implement cleanup
 * </pre>
 */
public abstract class Cancellable {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * Cancel the operation
     * Calls onCancel() only once on first invocation
     */
    public final void cancel() {
        // compareAndSet ensures onCancel() is called only once
        if (cancelled.compareAndSet(false, true)) {
            onCancel();
        }
    }

    /**
     * Check if cancellation was requested
     */
    public final boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * Subclasses implement cleanup logic here
     * Called exactly once when cancel() is first invoked
     */
    protected abstract void onCancel();
}