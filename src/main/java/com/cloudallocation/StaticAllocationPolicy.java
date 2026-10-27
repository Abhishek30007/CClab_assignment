package com.cloudallocation;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
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
import java.util.List;

/**
 * Static Resource Allocation Policy:
 * - Assigns initial VMs using fixed Round-Robin / First-Fit mapping (4 VMs per host across 5 hosts).
 * - Maintains initial VM-to-Host mapping strictly throughout the experiment without any VM migration.
 * - Collects performance metrics at monitoring intervals.
 */
public class StaticAllocationPolicy {

    private final CloudConfiguration config;

    public StaticAllocationPolicy(CloudConfiguration config) {
        this.config = config;
    }

    public MetricsCollector runSimulation(String experimentId, int runId, List<Cloudlet> workload) {
        CloudSimPlus simulation = new CloudSimPlus();

        // 1. Create Datacenter Broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 2. Create 5 Physical Hosts
        List<Host> hostList = HostFactory.createHosts(config);

        // 3. Static VM Allocation Policy (Round-Robin places exactly 4 VMs per host across 5 hosts)
        org.cloudsimplus.allocationpolicies.VmAllocationPolicyRoundRobin allocationPolicy =
                new org.cloudsimplus.allocationpolicies.VmAllocationPolicyRoundRobin();
        Datacenter datacenter = new DatacenterSimple(simulation, hostList, allocationPolicy);

        // 4. Create 20 VMs and submit
        List<Vm> vmList = VMFactory.createVms(config);
        broker.submitVmList(vmList);

        // 5. Submit Workload (deep copy cloudlets to maintain pristine state)
        List<Cloudlet> simulationWorkload = cloneWorkload(workload, vmList);
        for (int i = 0; i < simulationWorkload.size(); i++) {
            Cloudlet c = simulationWorkload.get(i);
            Cloudlet original = workload.get(i);
            if (original.getVm() != null && original.getVm().getId() >= 0 && original.getVm().getId() < vmList.size()) {
                c.setVm(vmList.get((int) original.getVm().getId()));
            }
        }
        broker.submitCloudletList(simulationWorkload);

        // 6. Metrics Collector & Periodic Monitoring
        MetricsCollector metrics = new MetricsCollector("Static", experimentId, runId, config);

        final double[] lastRecordedInterval = {-1.0};
        simulation.addOnClockTickListener(evt -> {
            double time = simulation.clock();
            double interval = config.getMonitoringInterval();
            long intervalIndex = (long) Math.floor(time / interval);
            if (time > 0 && intervalIndex > 0 && intervalIndex != (long) lastRecordedInterval[0]) {
                lastRecordedInterval[0] = intervalIndex;
                metrics.recordInterval(simulation, hostList, broker.getCloudletFinishedList());
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

            if (c.getVm() != null && c.getVm().getId() >= 0 && c.getVm().getId() < vmList.size()) {
                cl.setVm(vmList.get((int) c.getVm().getId()));
            }
            copy.add(cl);
        }
        return copy;
    }
}
