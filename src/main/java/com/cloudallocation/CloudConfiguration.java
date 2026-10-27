package com.cloudallocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Centralized Configuration Manager for Cloud Simulation.
 * Loads defaults or external configuration from config/simulation.properties.
 */
public class CloudConfiguration {
    private static CloudConfiguration instance;

    // Infrastructure
    private int hostCount = 5;
    private int hostPes = 8;
    private double hostMips = 2500;
    private long hostRamMb = 32768;
    private long hostBwMbps = 10000;
    private long hostStorageMb = 1000000;

    // VMs
    private int vmCount = 20;
    private int initialVmsPerHost = 4;
    private int vmPes = 2;
    private double vmMips = 2500;
    private long vmRamMb = 4096;
    private long vmBwMbps = 1000;
    private long vmStorageMb = 10000;

    // Simulation
    private double simulationDuration = 1200.0;
    private int cloudletCount = 500;
    private long cloudletBaseLength = 150000;
    private double slaThresholdSeconds = 0.50; // 500 ms SLA threshold
    private long randomSeed = 42;

    // Dynamic Policy
    private double cpuUpperThreshold = 0.80;
    private double cpuLowerThreshold = 0.30;
    private double monitoringInterval = 10.0;
    private boolean enableUnderloadConsolidation = false;

    private CloudConfiguration() {
        loadProperties("config/simulation.properties");
    }

    public static synchronized CloudConfiguration getInstance() {
        if (instance == null) {
            instance = new CloudConfiguration();
        }
        return instance;
    }

    public void loadProperties(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) {
            System.out.println("[Config] Config file not found at: " + f.getAbsolutePath());
            return;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(f)) {
            props.load(fis);
            this.hostCount = Integer.parseInt(props.getProperty("hosts.count", String.valueOf(hostCount)));
            this.hostPes = Integer.parseInt(props.getProperty("hosts.pes", String.valueOf(hostPes)));
            this.hostMips = Double.parseDouble(props.getProperty("hosts.mips", String.valueOf(hostMips)));
            this.hostRamMb = Long.parseLong(props.getProperty("hosts.ram.mb", String.valueOf(hostRamMb)));
            this.hostBwMbps = Long.parseLong(props.getProperty("hosts.bw.mbps", String.valueOf(hostBwMbps)));
            this.hostStorageMb = Long.parseLong(props.getProperty("hosts.storage.mb", String.valueOf(hostStorageMb)));

            this.vmCount = Integer.parseInt(props.getProperty("vms.count", String.valueOf(vmCount)));
            this.initialVmsPerHost = Integer.parseInt(props.getProperty("vms.initial.per.host", String.valueOf(initialVmsPerHost)));
            this.vmPes = Integer.parseInt(props.getProperty("vms.pes", String.valueOf(vmPes)));
            this.vmMips = Double.parseDouble(props.getProperty("vms.mips", String.valueOf(vmMips)));
            this.vmRamMb = Long.parseLong(props.getProperty("vms.ram.mb", String.valueOf(vmRamMb)));
            this.vmBwMbps = Long.parseLong(props.getProperty("vms.bw.mbps", String.valueOf(vmBwMbps)));
            this.vmStorageMb = Long.parseLong(props.getProperty("vms.size.mb", String.valueOf(vmStorageMb)));

            this.simulationDuration = Double.parseDouble(props.getProperty("simulation.duration.seconds", String.valueOf(simulationDuration)));
            this.cloudletCount = Integer.parseInt(props.getProperty("cloudlets.default.count", String.valueOf(cloudletCount)));
            this.cloudletBaseLength = Long.parseLong(props.getProperty("cloudlets.base.length", String.valueOf(cloudletBaseLength)));
            this.slaThresholdSeconds = Double.parseDouble(props.getProperty("sla.response.time.threshold", String.valueOf(slaThresholdSeconds)));
            this.randomSeed = Long.parseLong(props.getProperty("random.seed", String.valueOf(randomSeed)));

            this.cpuUpperThreshold = Double.parseDouble(props.getProperty("dynamic.cpu.upper.threshold", String.valueOf(cpuUpperThreshold)));
            this.cpuLowerThreshold = Double.parseDouble(props.getProperty("dynamic.cpu.lower.threshold", String.valueOf(cpuLowerThreshold)));
            this.monitoringInterval = Double.parseDouble(props.getProperty("dynamic.monitoring.interval.seconds", String.valueOf(monitoringInterval)));
            this.enableUnderloadConsolidation = Boolean.parseBoolean(props.getProperty("dynamic.enable.underload.consolidation", String.valueOf(enableUnderloadConsolidation)));
            System.out.printf("[Config] Loaded: hostCount=%d, hostPes=%d, hostMips=%.0f, vmPes=%d, vmMips=%.0f, duration=%.0f\n",
                    hostCount, hostPes, hostMips, vmPes, vmMips, simulationDuration);
        } catch (IOException | NumberFormatException e) {
            System.err.println("Warning: Could not read configuration file: " + e.getMessage() + ". Using defaults.");
        }
    }

    // Getters and Setters
    public int getHostCount() { return hostCount; }
    public void setHostCount(int hostCount) { this.hostCount = hostCount; }

    public int getHostPes() { return hostPes; }
    public void setHostPes(int hostPes) { this.hostPes = hostPes; }

    public double getHostMips() { return hostMips; }
    public void setHostMips(double hostMips) { this.hostMips = hostMips; }

    public long getHostRamMb() { return hostRamMb; }
    public void setHostRamMb(long hostRamMb) { this.hostRamMb = hostRamMb; }

    public long getHostBwMbps() { return hostBwMbps; }
    public void setHostBwMbps(long hostBwMbps) { this.hostBwMbps = hostBwMbps; }

    public long getHostStorageMb() { return hostStorageMb; }
    public void setHostStorageMb(long hostStorageMb) { this.hostStorageMb = hostStorageMb; }

    public int getVmCount() { return vmCount; }
    public void setVmCount(int vmCount) { this.vmCount = vmCount; }

    public int getInitialVmsPerHost() { return initialVmsPerHost; }
    public void setInitialVmsPerHost(int initialVmsPerHost) { this.initialVmsPerHost = initialVmsPerHost; }

    public int getVmPes() { return vmPes; }
    public void setVmPes(int vmPes) { this.vmPes = vmPes; }

    public double getVmMips() { return vmMips; }
    public void setVmMips(double vmMips) { this.vmMips = vmMips; }

    public long getVmRamMb() { return vmRamMb; }
    public void setVmRamMb(long vmRamMb) { this.vmRamMb = vmRamMb; }

    public long getVmBwMbps() { return vmBwMbps; }
    public void setVmBwMbps(long vmBwMbps) { this.vmBwMbps = vmBwMbps; }

    public long getVmStorageMb() { return vmStorageMb; }
    public void setVmStorageMb(long vmStorageMb) { this.vmStorageMb = vmStorageMb; }

    public double getSimulationDuration() { return simulationDuration; }
    public void setSimulationDuration(double simulationDuration) { this.simulationDuration = simulationDuration; }

    public int getCloudletCount() { return cloudletCount; }
    public void setCloudletCount(int cloudletCount) { this.cloudletCount = cloudletCount; }

    public long getCloudletBaseLength() { return cloudletBaseLength; }
    public void setCloudletBaseLength(long cloudletBaseLength) { this.cloudletBaseLength = cloudletBaseLength; }

    public double getSlaThresholdSeconds() { return slaThresholdSeconds; }
    public void setSlaThresholdSeconds(double slaThresholdSeconds) { this.slaThresholdSeconds = slaThresholdSeconds; }

    public long getRandomSeed() { return randomSeed; }
    public void setRandomSeed(long randomSeed) { this.randomSeed = randomSeed; }

    public double getCpuUpperThreshold() { return cpuUpperThreshold; }
    public void setCpuUpperThreshold(double cpuUpperThreshold) { this.cpuUpperThreshold = cpuUpperThreshold; }

    public double getCpuLowerThreshold() { return cpuLowerThreshold; }
    public void setCpuLowerThreshold(double cpuLowerThreshold) { this.cpuLowerThreshold = cpuLowerThreshold; }

    public double getMonitoringInterval() { return monitoringInterval; }
    public void setMonitoringInterval(double monitoringInterval) { this.monitoringInterval = monitoringInterval; }

    public boolean isEnableUnderloadConsolidation() { return enableUnderloadConsolidation; }
    public void setEnableUnderloadConsolidation(boolean enableUnderloadConsolidation) { this.enableUnderloadConsolidation = enableUnderloadConsolidation; }
}
