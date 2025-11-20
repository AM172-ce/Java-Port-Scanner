package scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Thread-safe implementation of ResultCollector
 */
public class ConcurrentResultCollector implements ResultCollector {
    private final ConcurrentLinkedQueue<ScanResult> results;

    public ConcurrentResultCollector() {
        this.results = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void addResult(ScanResult result) {
        if (result != null) {
            results.offer(result);
        }
    }

    @Override
    public void addResults(List<ScanResult> resultList) {
        if (resultList != null) {
            results.addAll(resultList);
        }
    }

    @Override
    public List<ScanResult> getResults() {
        return new ArrayList<>(results);
    }

    @Override
    public int size() {
        return results.size();
    }
}
