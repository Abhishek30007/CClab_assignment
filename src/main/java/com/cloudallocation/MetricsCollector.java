package com.cloudallocation;

import com.cloudallocation.models.IntervalMetric;
import com.cloudallocation.models.MigrationEvent;
import com.cloudallocation.models.TaskMetric;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.Simulation;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MetricsCollector tracks system-wide and host-level performance throughout simulation:
 * - Host CPU and Memory utilization per interval
 * - Response times and SLA violations
 * - Throughput (cloudlets completed per second)
 * - VM migrations
 * - Aggregated summary statistics
 */
public class MetricsCollector {
    private final String algorithm;
    private final String experimentId;
    private final int runId;
    private final CloudConfiguration config;

    private final List<IntervalMetric> intervalMetrics = new ArrayList<>();
    private final List<TaskMetric> taskMetrics = new ArrayList<>();
    private final List<MigrationEvent> migrationEvents = new ArrayList<>();

    private int previousFinishedCloudletCount = 0;
    private double lastIntervalTime = 0.0;

    public MetricsCollector(String algorithm, String experimentId, int runId, CloudConfiguration config) {
        this.algorithm = algorithm;
        this.experimentId = experimentId;
        this.runId = runId;
        this.config = config;
    }

    public void recordInterval(Simulation simulation, List<Host> hosts, List<Cloudlet> finishedCloudlets) {
        double currentTime = simulation.clock();
        if (currentTime <= 0.0 && !intervalMetrics.isEmpty()) {
            return;
        }

        double totalCpuUtil = 0.0;
        double maxCpuUtil = 0.0;
        double totalMemUtil = 0.0;

        for (Host host : hosts) {
            double hostCpu = host.getCpuPercentUtilization();
            if (Double.isNaN(hostCpu) || hostCpu < 0) hostCpu = 0.0;
            if (hostCpu > 1.0) hostCpu = 1.0;

            totalCpuUtil += hostCpu;
            if (hostCpu > maxCpuUtil) {
                maxCpuUtil = hostCpu;
            }

            double ramAllocated = host.getRam().getAllocatedResource();
            double ramTotal = host.getRam().getCapacity();
            double hostMem = ramTotal > 0 ? (ramAllocated / ramTotal) : 0.0;
            totalMemUtil += hostMem;
        }

        double avgCpu = hosts.isEmpty() ? 0.0 : (totalCpuUtil / hosts.size());
        double avgMem = hosts.isEmpty() ? 0.0 : (totalMemUtil / hosts.size());

        int currentFinishedCount = finishedCloudlets.size();
        int deltaFinished = currentFinishedCount - previousFinishedCloudletCount;
        double deltaTime = (currentTime - lastIntervalTime);
        double throughput = (deltaTime > 0) ? (deltaFinished / deltaTime) : 0.0;
        previousFinishedCloudletCount = currentFinishedCount;
        lastIntervalTime = currentTime;

        double sumResponseTime = 0.0;
        long intervalSlaViolations = 0;
        if (deltaFinished > 0) {
            int startIndex = Math.max(0, currentFinishedCount - deltaFinished);
            for (int i = startIndex; i < currentFinishedCount; i++) {
                Cloudlet c = finishedCloudlets.get(i);
                double responseTime = c.getFinishTime() - c.getSubmissionDelay();
                if (responseTime <= 0) responseTime = c.getFinishTime() - c.getStartTime();
                if (responseTime <= 0) responseTime = 0.01;
                sumResponseTime += responseTime;
                if (responseTime > config.getSlaThresholdSeconds()) {
                    intervalSlaViolations++;
                }
            }
        }
        double avgResponseTime = (deltaFinished > 0) ? (sumResponseTime / deltaFinished) : 0.0;
        long totalMigrationsSoFar = migrationEvents.stream().filter(e -> "SUCCESS".equals(e.getMigrationStatus())).count();

        IntervalMetric metric = new IntervalMetric(
                algorithm, experimentId, runId, currentTime,
                avgCpu, avgMem, maxCpuUtil,
                avgResponseTime, throughput,
                intervalSlaViolations, totalMigrationsSoFar
        );
        intervalMetrics.add(metric);
    }

    public void processCompletedTasks(List<Cloudlet> finishedCloudlets) {
        taskMetrics.clear();
        for (Cloudlet c : finishedCloudlets) {
            double arrivalTime = c.getSubmissionDelay();
            double startTime = c.getStartTime();
            double finishTime = c.getFinishTime();
            double responseTime = finishTime - arrivalTime;
            if (responseTime <= 0) responseTime = finishTime - startTime;
            if (responseTime <= 0) responseTime = 0.01;
            boolean slaViolated = (responseTime > config.getSlaThresholdSeconds());
            long vmId = c.getVm() != null ? c.getVm().getId() : -1;

            taskMetrics.add(new TaskMetric(
                    algorithm, experimentId, runId, c.getId(), vmId,
                    arrivalTime, startTime, finishTime, responseTime, slaViolated
            ));
        }
    }

    public void addMigrationEvent(double time, long vmId, long srcHostId, long dstHostId, String status) {
        migrationEvents.add(new MigrationEvent(runId, time, vmId, srcHostId, dstHostId, status));
    }

    public List<IntervalMetric> getIntervalMetrics() { return intervalMetrics; }
    public List<TaskMetric> getTaskMetrics() { return taskMetrics; }
    public List<MigrationEvent> getMigrationEvents() { return migrationEvents; }

    public double getAverageCpuUtilization() {
        if (intervalMetrics.isEmpty()) return 0.0;
        return intervalMetrics.stream().mapToDouble(IntervalMetric::getAvgCpu).average().orElse(0.0);
    }

    public double getPeakCpuUtilization() {
        if (intervalMetrics.isEmpty()) return 0.0;
        return intervalMetrics.stream().mapToDouble(IntervalMetric::getMaxCpu).max().orElse(0.0);
    }

    public double getAverageMemoryUtilization() {
        if (intervalMetrics.isEmpty()) return 0.0;
        return intervalMetrics.stream().mapToDouble(IntervalMetric::getAvgMemory).average().orElse(0.0);
    }

    public double getAverageResponseTime() {
        if (taskMetrics.isEmpty()) return 0.0;
        return taskMetrics.stream().mapToDouble(TaskMetric::getResponseTime).average().orElse(0.0);
    }

    public double getMedianResponseTime() {
        if (taskMetrics.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(taskMetrics.stream().map(TaskMetric::getResponseTime).toList());
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        } else {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        }
    }

    public double getP95ResponseTime() {
        if (taskMetrics.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(taskMetrics.stream().map(TaskMetric::getResponseTime).toList());
        Collections.sort(sorted);
        int index = (int) Math.ceil(0.95 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    public long getTotalSlaViolations() {
        return taskMetrics.stream().filter(TaskMetric::isSlaViolated).count();
    }

    public double getSlaViolationRate() {
        if (taskMetrics.isEmpty()) return 0.0;
        return (double) getTotalSlaViolations() / taskMetrics.size();
    }

    public long getSuccessfulMigrationsCount() {
        return migrationEvents.stream().filter(e -> "SUCCESS".equals(e.getMigrationStatus())).count();
    }

    public double getMakespan() {
        return taskMetrics.stream().mapToDouble(TaskMetric::getFinishTime).max().orElse(0.0);
    }

    public String getAlgorithm() { return algorithm; }
    public String getExperimentId() { return experimentId; }
    public int getRunId() { return runId; }
}
