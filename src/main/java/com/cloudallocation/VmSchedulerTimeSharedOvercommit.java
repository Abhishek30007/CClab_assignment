package com.cloudallocation;

import org.cloudsimplus.schedulers.MipsShare;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.vms.Vm;

/**
 * Time-shared VM Scheduler supporting realistic CPU overcommitment across VMs on physical hosts.
 * In production cloud environments (e.g. VMware ESXi, KVM, Xen), CPU cores are virtualized
 * and time-shared across multiple virtual machines. CloudSim Plus's default VmSchedulerTimeShared
 * prevents overcommitting requested MIPS; this scheduler allows true time-shared overcommitment
 * up to available physical processing elements (PEs).
 */
public class VmSchedulerTimeSharedOvercommit extends VmSchedulerTimeShared {

    public VmSchedulerTimeSharedOvercommit() {
        super();
    }

    public VmSchedulerTimeSharedOvercommit(double vmMigrationCpuOverhead) {
        super(vmMigrationCpuOverhead);
    }

    @Override
    protected boolean isSuitableForVmInternal(Vm vm, MipsShare requestedMips) {
        // True time-shared multi-tenancy: Host is suitable as long as physical PEs can host the VM
        return getHost().getWorkingPesNumber() >= requestedMips.pes();
    }
}
