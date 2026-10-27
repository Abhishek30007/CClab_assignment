package com.cloudallocation;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WorkloadGenerator creates computational tasks (Cloudlets) matching the target workload phases:
 * Phase A (0-300s): Low workload
 * Phase B (300-600s): Increasing workload
 * Phase C (600-900s): Peak workload
 * Phase D (900-1200s): Recovery
 */
public class WorkloadGenerator {

    public enum Scenario {
        E1_LOW_STABLE,
        E2_INCREASING,
        E3_PEAK,
        E4_PEAK_RECOVERY,
        E5_MIXED_RANDOM,
        CONTROLLED_HOTSPOT
    }

    public static List<Cloudlet> generateWorkload(CloudConfiguration config, Scenario scenario, long seed) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        Random random = new Random(seed);
        int totalTasks = config.getCloudletCount();
        long baseLength = config.getCloudletBaseLength();
        int pes = config.getVmPes();

        for (int i = 0; i < totalTasks; i++) {
            double arrivalTime;
            long taskLength;
            double targetCpuDemand;
            int preferredVmId = -1;

            switch (scenario) {
                case E1_LOW_STABLE:
                    arrivalTime = random.nextDouble() * 1150.0;
                    taskLength = (long) (baseLength * (0.3 + 0.2 * random.nextDouble()));
                    targetCpuDemand = 0.25;
                    break;

                case E2_INCREASING:
                    arrivalTime = random.nextDouble() * 1150.0;
                    double progress = arrivalTime / 1200.0;
                    taskLength = (long) (baseLength * (0.6 + 1.4 * progress));
                    targetCpuDemand = Math.min(1.0, 0.4 + 0.6 * progress);
                    // In later half of E2, cluster load on VMs on Host 0 and Host 1
                    if (progress > 0.5 && random.nextDouble() < 0.70) {
                        preferredVmId = (random.nextBoolean() ? 0 : 1) + (random.nextInt(4) * 5); // VMs on Host 0 or 1
                    } else {
                        preferredVmId = random.nextInt(config.getVmCount());
                    }
                    break;

                case E3_PEAK:
                    arrivalTime = random.nextDouble() * 1150.0;
                    taskLength = (long) (baseLength * (1.5 + 0.8 * random.nextDouble()));
                    targetCpuDemand = 0.95 + 0.05 * random.nextDouble();
                    // In peak, concentrated traffic arrives at subsets of VMs causing localized host overload
                    if (random.nextDouble() < 0.65) {
                        int hotspotHost = (i % 2); // Host 0 or Host 1
                        preferredVmId = hotspotHost + (random.nextInt(4) * 5);
                    } else {
                        preferredVmId = random.nextInt(config.getVmCount());
                    }
                    break;

                case E4_PEAK_RECOVERY:
                    double u = random.nextDouble();
                    if (u < 0.20) { // Phase A (0-300s)
                        arrivalTime = random.nextDouble() * 300.0;
                        taskLength = (long) (baseLength * (0.4 + 0.2 * random.nextDouble()));
                        targetCpuDemand = 0.25;
                        preferredVmId = random.nextInt(config.getVmCount());
                    } else if (u < 0.50) { // Phase B (300-600s)
                        arrivalTime = 300.0 + random.nextDouble() * 300.0;
                        taskLength = (long) (baseLength * (0.9 + 0.4 * random.nextDouble()));
                        targetCpuDemand = 0.80;
                        preferredVmId = (i % 2) + (random.nextInt(4) * 5); // Cluster on Host 0/1
                    } else if (u < 0.85) { // Phase C (600-900s)
                        arrivalTime = 600.0 + random.nextDouble() * 300.0;
                        taskLength = (long) (baseLength * (1.8 + 0.6 * random.nextDouble()));
                        targetCpuDemand = 1.0;
                        preferredVmId = (i % 2) + (random.nextInt(4) * 5); // Heavy peak on Host 0/1
                    } else { // Phase D (900-1200s)
                        arrivalTime = 900.0 + random.nextDouble() * 300.0;
                        taskLength = (long) (baseLength * (0.5 + 0.3 * random.nextDouble()));
                        targetCpuDemand = 0.35;
                        preferredVmId = random.nextInt(config.getVmCount());
                    }
                    break;

                case E5_MIXED_RANDOM:
                    arrivalTime = random.nextDouble() * 1150.0;
                    if (random.nextDouble() < 0.45) { // Burst spike
                        taskLength = (long) (baseLength * (1.8 + 0.8 * random.nextDouble()));
                        targetCpuDemand = 1.0;
                        preferredVmId = (random.nextInt(2)) + (random.nextInt(4) * 5); // Burst on Host 0 or 1
                    } else {
                        taskLength = (long) (baseLength * (0.5 + 0.3 * random.nextDouble()));
                        targetCpuDemand = 0.35;
                        preferredVmId = random.nextInt(config.getVmCount());
                    }
                    break;

                case CONTROLLED_HOTSPOT:
                default:
                    // Hotspot specifically targeting VMs 0, 5, 10, 15 (which reside on Host 0)
                    // with heavy computational demand to push Host 0 to 100% CPU utilization
                    int[] host0Vms = {0, 5, 10, 15};
                    if (i < 100) {
                        preferredVmId = host0Vms[i % 4];
                        // Waves of 4 simultaneous cloudlets starting at t=10, 40, 70, 100...
                        arrivalTime = 10.0 + (i / 4) * 30.0;
                        taskLength = (long) (baseLength * 4.0); // 600,000 MI = ~120s running time
                        targetCpuDemand = 1.0;
                    } else {
                        // Distribute across VMs on other hosts (Hosts 1, 2, 3, 4)
                        int[] otherVms = {1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14, 16, 17, 18, 19};
                        preferredVmId = otherVms[i % otherVms.length];
                        arrivalTime = 20.0 + random.nextDouble() * 900.0;
                        taskLength = (long) (baseLength * 0.4);
                        targetCpuDemand = 0.25;
                    }
                    break;
            }

            UtilizationModelDynamic cpuModel = new UtilizationModelDynamic(targetCpuDemand);
            Cloudlet cloudlet = new CloudletSimple(i, taskLength, pes);
            cloudlet.setUtilizationModelCpu(cpuModel);
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.0));
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.0));
            cloudlet.setSubmissionDelay(arrivalTime);

            if (preferredVmId >= 0 && preferredVmId < config.getVmCount()) {
                cloudlet.setVm(new VmSimple(preferredVmId, config.getVmMips(), config.getVmPes()));
            }

            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }
}
