package com.cloudallocation;

import com.cloudallocation.models.IntervalMetric;
import org.cloudsimplus.cloudlets.Cloudlet;

import java.util.ArrayList;
import java.util.List;

/**
 * ExperimentRunner coordinates running simulations for scenarios E1 to E5 (and controlled tests),
 * executing both Static and Dynamic policies against identical workload instances,
 * and generating aggregated results.
 */
public class ExperimentRunner {

    private final CloudConfiguration config;
    private final CSVExporter csvExporter;

    public ExperimentRunner(CloudConfiguration config, CSVExporter csvExporter) {
        this.config = config;
        this.csvExporter = csvExporter;
    }

    /**
     * Run a specific scenario for a given number of repetitions.
     */
    public void runScenario(WorkloadGenerator.Scenario scenario, int repetitions) {
        String scenarioName = scenario.name();
        System.out.println("\n========================================================");
        System.out.println(">>> RUNNING SCENARIO: " + scenarioName + " (" + repetitions + " repetitions)");
        System.out.println("========================================================");

        StaticAllocationPolicy staticPolicy = new StaticAllocationPolicy(config);
        DynamicAllocationPolicy dynamicPolicy = new DynamicAllocationPolicy(config);

        for (int run = 1; run <= repetitions; run++) {
            long seed = config.getRandomSeed() + run * 100L;
            System.out.printf("\n--- [%s] Run #%d / %d (Seed: %d) ---\n", scenarioName, run, repetitions, seed);

            // Generate equivalent baseline workload once for both policies
            List<Cloudlet> baseWorkload = WorkloadGenerator.generateWorkload(config, scenario, seed);

            // 1. Run Static Allocation Simulation
            System.out.println("[Static] Executing static allocation simulation...");
            MetricsCollector staticMetrics = staticPolicy.runSimulation(scenarioName, run, baseWorkload);
            System.out.printf("[Static Result] Avg CPU: %.2f%%, Peak CPU: %.2f%%, Avg Response: %.4fs, SLA Violations: %d\n",
                    staticMetrics.getAverageCpuUtilization() * 100.0,
                    staticMetrics.getPeakCpuUtilization() * 100.0,
                    staticMetrics.getAverageResponseTime(),
                    staticMetrics.getTotalSlaViolations());

            // 2. Run Dynamic Allocation Simulation
            System.out.println("[Dynamic] Executing dynamic allocation simulation with VM migration...");
            MetricsCollector dynamicMetrics = dynamicPolicy.runSimulation(scenarioName, run, baseWorkload);
            System.out.printf("[Dynamic Result] Avg CPU: %.2f%%, Peak CPU: %.2f%%, Migrations: %d, Avg Response: %.4fs, SLA Violations: %d\n",
                    dynamicMetrics.getAverageCpuUtilization() * 100.0,
                    dynamicMetrics.getPeakCpuUtilization() * 100.0,
                    dynamicMetrics.getSuccessfulMigrationsCount(),
                    dynamicMetrics.getAverageResponseTime(),
                    dynamicMetrics.getTotalSlaViolations());

            // 3. Export to CSV files
            exportMetrics(staticMetrics, dynamicMetrics, scenarioName, run);
        }
    }

    private void exportMetrics(MetricsCollector staticMetrics, MetricsCollector dynamicMetrics, String scenarioName, int run) {
        // Individual interval files
        csvExporter.writeIntervalResults("static_results.csv", staticMetrics.getIntervalMetrics(), true);
        csvExporter.writeIntervalResults("dynamic_results.csv", dynamicMetrics.getIntervalMetrics(), true);

        // Combined interval file
        List<IntervalMetric> combined = new ArrayList<>();
        combined.addAll(staticMetrics.getIntervalMetrics());
        combined.addAll(dynamicMetrics.getIntervalMetrics());
        csvExporter.writeIntervalResults("combined_results.csv", combined, true);

        // Task metrics
        csvExporter.writeTaskResults(staticMetrics.getTaskMetrics(), true);
        csvExporter.writeTaskResults(dynamicMetrics.getTaskMetrics(), true);

        // Migration logs
        csvExporter.writeMigrationLogs(dynamicMetrics.getMigrationEvents(), true);

        // Summary row for Static
        String staticSummary = String.format("%s,%d,Static,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%.4f,%d,%.4f,%d,%.2f",
                scenarioName, run,
                staticMetrics.getAverageCpuUtilization(),
                staticMetrics.getPeakCpuUtilization(),
                staticMetrics.getAverageMemoryUtilization(),
                staticMetrics.getAverageResponseTime(),
                staticMetrics.getMedianResponseTime(),
                staticMetrics.getP95ResponseTime(),
                staticMetrics.getTaskMetrics().size(),
                staticMetrics.getIntervalMetrics().stream().mapToDouble(IntervalMetric::getThroughput).average().orElse(0.0),
                staticMetrics.getTotalSlaViolations(),
                staticMetrics.getSlaViolationRate(),
                staticMetrics.getSuccessfulMigrationsCount(),
                staticMetrics.getMakespan());
        csvExporter.writeSummaryRow(staticSummary, true);

        // Summary row for Dynamic
        String dynamicSummary = String.format("%s,%d,Dynamic,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%.4f,%d,%.4f,%d,%.2f",
                scenarioName, run,
                dynamicMetrics.getAverageCpuUtilization(),
                dynamicMetrics.getPeakCpuUtilization(),
                dynamicMetrics.getAverageMemoryUtilization(),
                dynamicMetrics.getAverageResponseTime(),
                dynamicMetrics.getMedianResponseTime(),
                dynamicMetrics.getP95ResponseTime(),
                dynamicMetrics.getTaskMetrics().size(),
                dynamicMetrics.getIntervalMetrics().stream().mapToDouble(IntervalMetric::getThroughput).average().orElse(0.0),
                dynamicMetrics.getTotalSlaViolations(),
                dynamicMetrics.getSlaViolationRate(),
                dynamicMetrics.getSuccessfulMigrationsCount(),
                dynamicMetrics.getMakespan());
        csvExporter.writeSummaryRow(dynamicSummary, true);
    }

    /**
     * Run the complete suite of standard experiments (E1 to E5).
     */
    public void runAllExperiments(int repetitionsPerScenario) {
        csvExporter.clearResultsDirectory();
        System.out.println("Starting full academic simulation experiment suite (E1 to E5)...");

        // First run controlled hotspot to verify deterministic VM migration
        runScenario(WorkloadGenerator.Scenario.CONTROLLED_HOTSPOT, 1);

        // Run E1 to E5
        runScenario(WorkloadGenerator.Scenario.E1_LOW_STABLE, repetitionsPerScenario);
        runScenario(WorkloadGenerator.Scenario.E2_INCREASING, repetitionsPerScenario);
        runScenario(WorkloadGenerator.Scenario.E3_PEAK, repetitionsPerScenario);
        runScenario(WorkloadGenerator.Scenario.E4_PEAK_RECOVERY, repetitionsPerScenario);
        runScenario(WorkloadGenerator.Scenario.E5_MIXED_RANDOM, repetitionsPerScenario);

        System.out.println("\n>>> All experiments completed successfully. Results exported to CSV files.");
    }
}
