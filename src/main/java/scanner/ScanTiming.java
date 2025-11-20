package scanner;

/**
 * Immutable configuration class
 */
public record ScanTiming(
        String name,
        int initialTimeout,
        int portThreadPoolSize,
        int targetThreadPoolSize,
        long shutdownTimeoutMinutes
){
    public ScanTiming{
        if(portThreadPoolSize <= 0 || targetThreadPoolSize <= 0){
            throw new IllegalArgumentException("Thread pool size must be positive");
        }
        if (initialTimeout <= 0) {
            throw new IllegalArgumentException("initialTimeoutMs must be positive");
        }
        if (shutdownTimeoutMinutes <= 0) {
            throw new IllegalArgumentException("Shutdown timeout must be positive");
        }
    }
    /**
     * T1 - Sneaky: Slow, less aggressive
     */
    public static ScanTiming SNEAKY() {
        return new ScanTiming(
                "T1-SNEAKY",
                3000,
                25,
                2,
                20
        );
    }

    /**
     * T2 - Polite: Slower than normal
     */
    public static ScanTiming POLITE() {
        return new ScanTiming(
                "T2-POLITE",
                1500,
                50,
                5,
                15
        );
    }


    public static ScanTiming NORMAL() {
        return new ScanTiming(
                "T3-NORMAL",
                1000,
                100,
                10,
                10
        );
    }

    /**
     * T4 - Aggressive: Fast scan
     */
    public static ScanTiming AGGRESSIVE() {
        return new ScanTiming(
                "T4-AGGRESSIVE",
                500,
                200,
                20,
                5
        );
    }



    public static ScanTiming.Builder custom(String name) {
        return new ScanTiming.Builder(name);
    }



    public static class Builder {
        private final String name;
        private int initialTimeout = 1000;
        private int portThreadPoolSize = 100;
        private int targetThreadPoolSize = 10;
        private long shutdownTimeoutMinutes = 10;

        public Builder(String name){
            this.name = name;
        }

        public ScanTiming.Builder initialTimeout(int timout){
            this.initialTimeout = timout;
            return this;
        }

        public Builder portThreadPoolSize(int size) {
            this.portThreadPoolSize = size;
            return this;
        }

        public Builder targetThreadPoolSize(int size) {
            this.targetThreadPoolSize = size;
            return this;
        }


        public Builder shutdownTimeout(long minutes) {
            this.shutdownTimeoutMinutes = minutes;
            return this;
        }

        public ScanTiming build(){
            return new ScanTiming(
                    name,
                    initialTimeout,
                    portThreadPoolSize,
                    targetThreadPoolSize,
                    shutdownTimeoutMinutes
            );
        }
    }
    @Override
    public String toString() {
        return String.format("%s (timeout=%dms, portThreads=%d, targetThreads=%d)",
                name, initialTimeout, portThreadPoolSize, targetThreadPoolSize);
    }
}