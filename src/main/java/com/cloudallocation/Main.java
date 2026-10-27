package com.cloudallocation;

import java.util.Arrays;

/**
 * Main entry point for Cloud Resource Allocation Simulation.
 * Supports running full experiments, controlled migration test, or custom scenarios.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CLOUD RESOURCE ALLOCATION SIMULATION: STATIC VS DYNAMIC (CLOUDSIM PLUS) ");
        System.out.println("==========================================================================");

        CloudConfiguration config = CloudConfiguration.getInstance();
        CSVExporter csvExporter = new CSVExporter("results");
        ExperimentRunner runner = new ExperimentRunner(config, csvExporter);

        if (args.length > 0 && "test".equalsIgnoreCase(args[0])) {
            System.out.println("Running Controlled Migration Test...");
            runner.runScenario(WorkloadGenerator.Scenario.CONTROLLED_HOTSPOT, 1);
        } else if (args.length > 0 && "quick".equalsIgnoreCase(args[0])) {
            System.out.println("Running Quick Experiment Suite (1 run per scenario)...");
            runner.runAllExperiments(1);
        } else {
            // Default: Full experiment suite (3 runs per scenario for comprehensive data)
            System.out.println("Running Full Academic Experiment Suite (3 runs per scenario)...");
            runner.runAllExperiments(3);
        }

        System.out.println("\nSimulation complete. Results available in 'results/' directory.");
        System.out.println("To start the Streamlit Dashboard, run: streamlit run dashboard/app.py");
    }
}
