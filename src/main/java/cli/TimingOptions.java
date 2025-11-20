package cli;

import picocli.CommandLine;
import scanner.ScanTiming;

public class TimingOptions{

    @CommandLine.Option(
            names = {"-T1"},
            description = "Timing Policy sneaky timout=3s"
    )
    private boolean t1Policy;

    @CommandLine.Option(
            names = {"-T2"},
            description = "Timing Policy polite timout=1.5s"
    )
    private boolean t2Policy;

    @CommandLine.Option(
            names = {"-T4"},
            description = "Timing Policy aggressive timout=0.5s"
    )
    private boolean t4Policy;


    @CommandLine.Option(
            names = {"--custom-timing"},
            description = "Timing Policy custom"
    )
    private boolean customPolicy;

    @CommandLine.Option(
            names = {"-t", "--timout"},
            description = "timout for custom timing"
    )
    private int timout;

    @CommandLine.Option(
            names = {"--port-threads"},
            description = "threads for ports over single target"
    )
    private int portThreadSize;

    @CommandLine.Option(
            names = {"--target-threads"},
            description = "threads over targets"
    )
    private int targetThreadSize;

    public ScanTiming getConfig(){
        if(t1Policy)
            return ScanTiming.SNEAKY();
        if(t2Policy)
            return ScanTiming.POLITE();
        if(t4Policy)
            return ScanTiming.AGGRESSIVE();
        if(customPolicy){
            if(errorForIncorrectInput() != null){
                return ScanTiming.custom("user-custom-timing")
                        .initialTimeout(timout)
                        .portThreadPoolSize(portThreadSize)
                        .targetThreadPoolSize(targetThreadSize)
                        .build();
            } else {
                return null;
            }
        }
        return ScanTiming.NORMAL();
    }
    private boolean isNotPositive(int input){
        return input <= 0;
    }
    public String errorForIncorrectInput(){
        if(customPolicy){
            if(isNotPositive(timout)){
                return "Timeout must be positive";
            }
            if(isNotPositive(portThreadSize) || isNotPositive(targetThreadSize)){
                return "Thread pool size must be positive";
            }
        }
        return null;
    }










}
