package com.cloudallocation;

import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.power.models.PowerModelHostSimple;
import org.cloudsimplus.provisioners.PeProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating homogeneous physical hosts in CloudSim Plus.
 */
public class HostFactory {

    public static List<Host> createHosts(CloudConfiguration config) {
        List<Host> hostList = new ArrayList<>(config.getHostCount());
        for (int i = 0; i < config.getHostCount(); i++) {
            List<Pe> peList = new ArrayList<>(config.getHostPes());
            for (int j = 0; j < config.getHostPes(); j++) {
                peList.add(new PeSimple(config.getHostMips(), new PeProvisionerSimple()));
            }

            Host host = new HostSimple(config.getHostRamMb(), config.getHostBwMbps(), config.getHostStorageMb(), peList);
            host.setId(i);
            host.setVmScheduler(new VmSchedulerTimeSharedOvercommit());
            // PowerModelHostSimple activates CPU and power utilization tracking
            host.setPowerModel(new PowerModelHostSimple(1000, 500));
            hostList.add(host);
        }
        return hostList;
    }
}
