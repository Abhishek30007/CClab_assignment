package com.cloudallocation.models;

/**
 * Data model for VM migration events.
 */
public class MigrationEvent {
    private int runId;
    private double timestamp;
    private long vmId;
    private long sourceHostId;
    private long destinationHostId;
    private String migrationStatus;

    public MigrationEvent(int runId, double timestamp, long vmId,
                          long sourceHostId, long destinationHostId, String migrationStatus) {
        this.runId = runId;
        this.timestamp = timestamp;
        this.vmId = vmId;
        this.sourceHostId = sourceHostId;
        this.destinationHostId = destinationHostId;
        this.migrationStatus = migrationStatus;
    }

    public String toCsvRow() {
        return String.format("%d,%.2f,%d,%d,%d,%s",
                runId, timestamp, vmId, sourceHostId, destinationHostId, migrationStatus);
    }

    public static String getCsvHeader() {
        return "run_id,timestamp,vm_id,source_host_id,destination_host_id,migration_status";
    }

    public int getRunId() { return runId; }
    public double getTimestamp() { return timestamp; }
    public long getVmId() { return vmId; }
    public long getSourceHostId() { return sourceHostId; }
    public long getDestinationHostId() { return destinationHostId; }
    public String getMigrationStatus() { return migrationStatus; }
}
