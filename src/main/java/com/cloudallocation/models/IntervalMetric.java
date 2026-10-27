package com.cloudallocation.models;

/**
 * Data model for recording simulation metrics at monitoring intervals.
 */
public class IntervalMetric {
    private String algorithm;
    private String experimentId;
    private int runId;
    private double time;
    private double avgCpu;
    private double avgMemory;
    private double maxCpu;
    private double responseTimeAvg;
    private double throughput;
    private long slaViolations;
    private long vmMigrations;

    public IntervalMetric(String algorithm, String experimentId, int runId, double time,
                          double avgCpu, double avgMemory, double maxCpu,
                          double responseTimeAvg, double throughput,
                          long slaViolations, long vmMigrations) {
        this.algorithm = algorithm;
        this.experimentId = experimentId;
        this.runId = runId;
        this.time = time;
        this.avgCpu = avgCpu;
        this.avgMemory = avgMemory;
        this.maxCpu = maxCpu;
        this.responseTimeAvg = responseTimeAvg;
        this.throughput = throughput;
        this.slaViolations = slaViolations;
        this.vmMigrations = vmMigrations;
    }

    public String toCsvRow() {
        return String.format("%s,%s,%d,%.2f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%d",
                algorithm, experimentId, runId, time, avgCpu, avgMemory, maxCpu,
                responseTimeAvg, throughput, slaViolations, vmMigrations);
    }

    public static String getCsvHeader() {
        return "algorithm,experiment_id,run_id,time,avg_cpu,avg_memory,max_cpu,response_time_avg,throughput,sla_violations,vm_migrations";
    }

    public String getAlgorithm() { return algorithm; }
    public String getExperimentId() { return experimentId; }
    public int getRunId() { return runId; }
    public double getTime() { return time; }
    public double getAvgCpu() { return avgCpu; }
    public double getAvgMemory() { return avgMemory; }
    public double getMaxCpu() { return maxCpu; }
    public double getResponseTimeAvg() { return responseTimeAvg; }
    public double getThroughput() { return throughput; }
    public long getSlaViolations() { return slaViolations; }
    public long getVmMigrations() { return vmMigrations; }
}
