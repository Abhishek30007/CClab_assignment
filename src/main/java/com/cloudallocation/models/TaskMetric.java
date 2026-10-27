package com.cloudallocation.models;

/**
 * Data model for individual cloudlet / task execution outcomes.
 */
public class TaskMetric {
    private String algorithm;
    private String experimentId;
    private int runId;
    private long taskId;
    private long vmId;
    private double arrivalTime;
    private double startTime;
    private double finishTime;
    private double responseTime;
    private boolean slaViolated;

    public TaskMetric(String algorithm, String experimentId, int runId, long taskId, long vmId,
                      double arrivalTime, double startTime, double finishTime,
                      double responseTime, boolean slaViolated) {
        this.algorithm = algorithm;
        this.experimentId = experimentId;
        this.runId = runId;
        this.taskId = taskId;
        this.vmId = vmId;
        this.arrivalTime = arrivalTime;
        this.startTime = startTime;
        this.finishTime = finishTime;
        this.responseTime = responseTime;
        this.slaViolated = slaViolated;
    }

    public String toCsvRow() {
        return String.format("%s,%s,%d,%d,%d,%.2f,%.2f,%.2f,%.4f,%b",
                algorithm, experimentId, runId, taskId, vmId,
                arrivalTime, startTime, finishTime, responseTime, slaViolated);
    }

    public static String getCsvHeader() {
        return "algorithm,experiment_id,run_id,task_id,vm_id,arrival_time,start_time,finish_time,response_time,sla_violated";
    }

    public String getAlgorithm() { return algorithm; }
    public String getExperimentId() { return experimentId; }
    public int getRunId() { return runId; }
    public long getTaskId() { return taskId; }
    public long getVmId() { return vmId; }
    public double getArrivalTime() { return arrivalTime; }
    public double getStartTime() { return startTime; }
    public double getFinishTime() { return finishTime; }
    public double getResponseTime() { return responseTime; }
    public boolean isSlaViolated() { return slaViolated; }
}
