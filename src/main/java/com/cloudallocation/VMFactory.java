package com.cloudallocation;

import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating Virtual Machines in CloudSim Plus.
 */
public class VMFactory {

    public static List<Vm> createVms(CloudConfiguration config) {
        List<Vm> vmList = new ArrayList<>(config.getVmCount());
        for (int i = 0; i < config.getVmCount(); i++) {
            Vm vm = new VmSimple(i, config.getVmMips(), config.getVmPes());
            vm.setRam(config.getVmRamMb())
              .setBw(config.getVmBwMbps())
              .setSize(config.getVmStorageMb())
              .setCloudletScheduler(new CloudletSchedulerTimeShared());
            vmList.add(vm);
        }
        return vmList;
    }
}
