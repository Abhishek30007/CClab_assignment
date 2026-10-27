package com.cloudallocation;

import org.cloudsimplus.cloudlets.Cloudlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Academic unit and integration tests verifying the 14 requirements specified
 * in the project specifications.
 */
public class CloudResourceAllocationTest {

    private CloudConfiguration config;

    @BeforeEach
    public void setUp() {
        config = CloudConfiguration.getInstance();
    }

    @Test
    @DisplayName("Test 1 & 2: Configuration loads 5 physical hosts and 20 VMs")
    public void testInfrastructureConfig() {
        assertEquals(5, config.getHostCount(), "Should configure 5 physical hosts");
        assertEquals(20, config.getVmCount(), "Should configure 20 virtual machines");
        assertEquals(8, config.getHostPes(), "Host should have 8 PEs");
        assertEquals(2, config.getVmPes(), "VM should have 2 PEs");
    }

    @Test
    @DisplayName("Test 3 & 4: Workload generation creates specified task count with valid configurations")
    public void testWorkloadGeneration() {
        List<Cloudlet> workload = WorkloadGenerator.generateWorkload(config, WorkloadGenerator.Scenario.E1_LOW_STABLE, 42L);
        assertNotNull(workload);
        assertEquals(config.getCloudletCount(), workload.size(), "Should generate configured cloudlet count");

        for (Cloudlet c : workload) {
            assertTrue(c.getLength() > 0, "Cloudlet length must be positive");
            assertEquals(config.getVmPes(), c.getPesNumber(), "Cloudlet PEs must match VM PEs");
        }
    }

    @Test
    @DisplayName("Test 5 & 6: Static policy runs and produces valid metrics within [0, 100%]")
    public void testStaticAllocationExecution() {
        StaticAllocationPolicy staticPolicy = new StaticAllocationPolicy(config);
        List<Cloudlet> workload = WorkloadGenerator.generateWorkload(config, WorkloadGenerator.Scenario.E1_LOW_STABLE, 100L);

        MetricsCollector metrics = staticPolicy.runSimulation("TEST_STATIC", 1, workload);
        assertNotNull(metrics);
        assertTrue(metrics.getAverageCpuUtilization() >= 0.0 && metrics.getAverageCpuUtilization() <= 1.0,
                "Average CPU utilization must be within [0.0, 1.0]");
        assertTrue(metrics.getPeakCpuUtilization() >= 0.0 && metrics.getPeakCpuUtilization() <= 1.0,
                "Peak CPU utilization must be within [0.0, 1.0]");
        assertEquals(0, metrics.getSuccessfulMigrationsCount(), "Static policy must perform zero migrations");
    }

    @Test
    @DisplayName("Test 7 & 8: Dynamic policy triggers deterministic overload detection and whole-VM migration")
    public void testDynamicMigrationDeterministic() {
        DynamicAllocationPolicy dynamicPolicy = new DynamicAllocationPolicy(config);
        List<Cloudlet> hotspotWorkload = WorkloadGenerator.generateWorkload(config, WorkloadGenerator.Scenario.CONTROLLED_HOTSPOT, 42L);

        MetricsCollector metrics = dynamicPolicy.runSimulation("TEST_HOTSPOT", 1, hotspotWorkload);
        assertNotNull(metrics);
        assertTrue(metrics.getSuccessfulMigrationsCount() >= 1,
                "Dynamic policy must detect hotspot overload and execute at least one whole-VM migration");
    }
}
