package scanner;

import java.util.List;

public interface ResultCollector {
    void addResult(ScanResult result);
    void addResults(List<ScanResult> results);
    List<ScanResult> getResults();
    int size();
}
