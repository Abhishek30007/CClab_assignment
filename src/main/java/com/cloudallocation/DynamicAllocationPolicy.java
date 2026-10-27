package com.cloudallocation;

import com.cloudallocation.models.MigrationEvent;
import org.cloudsimplus.allocationpolicies.VmAllocationPolicyRoundRobin;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Resource Allocation Policy:
 * Implements the core requirements:
 * 1. Collects CPU utilization per physical host at monitoring intervals.
 * 2. Computes host utilization percentage.
 * 3. Identifies hosts exceeding the upper CPU threshold (e.g., 80%).
 * 4. Identifies VMs on the overloaded host as migration candidates.
 * 5. Evaluates candidate resource demand.
 * 6. Finds a feasible destination host with sufficient available CPU and RAM.
 * 7. Evaluates destination load and selects a destination that improves load balance.
 * 8. Migrates the entire VM between physical hosts using CloudSim Plus requestVmMigration.
 * 9. Verifies whole-VM placement on the destination host via migration finish listener.
 * 10. Records source host, destination host, VM ID, simulation timestamp, and status.
 */
public class DynamicAllocationPolicy {

    private final CloudConfiguration config;

    public DynamicAllocationPolicy(CloudConfiguration config) {
        this.config = config;
    }

    public MetricsCollector runSimulation(String experimentId, int runId, List<Cloudlet> workload) {
        CloudSimPlus simulation = new CloudSimPlus();

        // 1. Create Datacenter Broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 2. Create 5 Physical Hosts
        List<Host> hostList = HostFactory.createHosts(config);

        // 3. Initial placement with Round-Robin (4 VMs per host initially, matching baseline)
        VmAllocationPolicyRoundRobin initialAllocationPolicy = new VmAllocationPolicyRoundRobin();
        Datacenter datacenter = new DatacenterSimple(simulation, hostList, initialAllocationPolicy);
        datacenter.setBandwidthPercentForMigration(0.50);
        datacenter.setSchedulingInterval(config.getMonitoringInterval());

        // 4. Create 20 VMs and register migration tracking
        List<Vm> vmList = VMFactory.createVms(config);
        MetricsCollector metrics = new MetricsCollector("Dynamic", experimentId, runId, config);

        Map<Long, Host> previousHostMap = new HashMap<>();

        // Register datacenter VM migration finish listener
        datacenter.addOnVmMigrationFinishListener(info -> {
            Vm vm = info.getVm();
            Host oldHost = previousHostMap.get(vm.getId());
            long srcHostId = (oldHost != null) ? oldHost.getId() : -1;
            long dstHostId = (vm.getHost() != null) ? vm.getHost().getId() : -1;
            double time = info.getTime();
            String status = info.isMigrationSuccessful() ? "SUCCESS" : "FAILED";

            metrics.addMigrationEvent(time, vm.getId(), srcHostId, dstHostId, status);
            if (info.isMigrationSuccessful()) {
                System.out.printf("[DynamicPolicy] Time %.2fs: Verified whole-VM Migration of VM #%d from Host #%d -> Host #%d\n",
                        time, vm.getId(), srcHostId, dstHostId);
                previousHostMap.put(vm.getId(), vm.getHost());
            }
        });

        broker.submitVmList(vmList);

        // 5. Submit Workload (cloned copy)
        List<Cloudlet> simulationWorkload = cloneWorkload(workload, vmList);
        for (int i = 0; i < simulationWorkload.size(); i++) {
            Cloudlet c = simulationWorkload.get(i);
            Cloudlet original = workload.get(i);
            if (original.getVm() != null && original.getVm().getId() >= 0 && original.getVm().getId() < vmList.size()) {
                c.setVm(vmList.get((int) original.getVm().getId()));
            }
        }
        broker.submitCloudletList(simulationWorkload);

        broker.addOnVmsCreatedListener(evt -> {
            for (Vm vm : vmList) {
                if (vm.getHost() != null) {
                    previousHostMap.put(vm.getId(), vm.getHost());
                }
            }
        });

        // Cooldown tracker to prevent repeated thrashing / ping-pong oscillations
        Map<Long, Double> vmLastMigrationTime = new HashMap<>();
        final double MIGRATION_COOLDOWN_SECONDS = 30.0;

        // 6. Active Dynamic Overload Monitor:
        // Periodically runs overload detection algorithm and whole-VM migration
        final double[] lastMonitoredInterval = {-1.0};
        simulation.addOnClockTickListener(evt -> {
            double time = simulation.clock();
            double interval = config.getMonitoringInterval();
            long intervalIndex = (long) Math.floor(time / interval);
            if (time > 0 && intervalIndex > 0 && intervalIndex != (long) lastMonitoredInterval[0]) {
                lastMonitoredInterval[0] = intervalIndex;
                // Record interval metrics
                metrics.recordInterval(simulation, hostList, broker.getCloudletFinishedList());

                // Evaluate every host for overload
                for (Host host : hostList) {
                    double cpuUsage = host.getCpuPercentUtilization();

                    if (cpuUsage >= config.getCpuUpperThreshold()) {
                        List<Vm> candidateVms = host.getVmCreatedList();
                        if (candidateVms.isEmpty()) continue;

                        // Step 4-8: Evaluate candidate VMs (not in cooldown, not currently migrating)
                        Vm selectedCandidate = null;
                        Host bestDestination = null;

                        for (int idx = candidateVms.size() - 1; idx >= 0; idx--) {
                            Vm v = candidateVms.get(idx);
                            if (v.isInMigration() || host.getVmsMigratingOut().contains(v)) {
                                continue;
                            }
                            double lastMig = vmLastMigrationTime.getOrDefault(v.getId(), -999.0);
                            if (time - lastMig < MIGRATION_COOLDOWN_SECONDS) {
                                continue;
                            }

                            // Evaluate destination hosts
                            double lowestDestLoad = Double.MAX_VALUE;
                            Host candidateDest = null;
                            for (Host dest : hostList) {
                                if (dest.getId() == host.getId()) continue;
                                if (dest.isSuitableForVm(v)) {
                                    double destLoad = dest.getCpuPercentUtilization();
                                    if (destLoad < lowestDestLoad && destLoad < config.getCpuUpperThreshold()) {
                                        lowestDestLoad = destLoad;
                                        candidateDest = dest;
                                    }
                                }
                            }

                            if (candidateDest != null) {
                                selectedCandidate = v;
                                bestDestination = candidateDest;
                                break; // Found feasible migration pair
                            }
                        }

                        if (selectedCandidate != null && bestDestination != null) {
                            System.out.printf("[DynamicMonitor] Time %.2fs: Overload detected on Host #%d (CPU: %.2f%% >= %.2f%%). Initiating whole-VM Migration of VM #%d to Host #%d (Current CPU: %.2f%%)\n",
                                    time, host.getId(), cpuUsage * 100.0, config.getCpuUpperThreshold() * 100.0,
                                    selectedCandidate.getId(), bestDestination.getId(), bestDestination.getCpuPercentUtilization() * 100.0);

                            vmLastMigrationTime.put(selectedCandidate.getId(), time);
                            datacenter.requestVmMigration(selectedCandidate, bestDestination);
                        } else if (!candidateVms.isEmpty()) {
                            Vm fallbackCand = candidateVms.get(0);
                            metrics.addMigrationEvent(time, fallbackCand.getId(), host.getId(), -1, "SKIPPED_NO_FEASIBLE_DESTINATION");
                        }
                    }
                }
            }
        });

        // 7. Execute Simulation
        simulation.terminateAt(config.getSimulationDuration());
        simulation.start();

        // Final recording
        metrics.recordInterval(simulation, hostList, broker.getCloudletFinishedList());
        metrics.processCompletedTasks(broker.getCloudletFinishedList());

        return metrics;
    }

    private List<Cloudlet> cloneWorkload(List<Cloudlet> workload, List<Vm> vmList) {
        List<Cloudlet> copy = new ArrayList<>(workload.size());
        for (Cloudlet c : workload) {
            CloudletSimple cl = new CloudletSimple(c.getId(), c.getLength(), c.getPesNumber());
            cl.setUtilizationModelCpu(c.getUtilizationModelCpu());
            cl.setUtilizationModelRam(c.getUtilizationModelRam());
            cl.setUtilizationModelBw(c.getUtilizationModelBw());
            cl.setSubmissionDelay(c.getSubmissionDelay());
            copy.add(cl);
        }
        return copy;
    }
}
