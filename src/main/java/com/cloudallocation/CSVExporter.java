package com.cloudallocation;

import com.cloudallocation.models.IntervalMetric;
import com.cloudallocation.models.MigrationEvent;
import com.cloudallocation.models.TaskMetric;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * CSVExporter writes simulation metrics to the required CSV structure:
 * - results/static_results.csv
 * - results/dynamic_results.csv
 * - results/combined_results.csv
 * - results/task_results.csv
 * - results/migration_log.csv
 * - results/experiment_summary.csv
 */
public class CSVExporter {

    private final String outputDir;

    public CSVExporter(String outputDir) {
        this.outputDir = outputDir;
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized void writeIntervalResults(String filename, List<IntervalMetric> metrics, boolean append) {
        File file = new File(outputDir, filename);
        boolean writeHeader = !file.exists() || !append || file.length() == 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, append))) {
            if (writeHeader) {
                pw.println(IntervalMetric.getCsvHeader());
            }
            for (IntervalMetric m : metrics) {
                pw.println(m.toCsvRow());
            }
        } catch (IOException e) {
            System.err.println("Error writing to " + filename + ": " + e.getMessage());
        }
    }

    public synchronized void writeTaskResults(List<TaskMetric> metrics, boolean append) {
        File file = new File(outputDir, "task_results.csv");
        boolean writeHeader = !file.exists() || !append || file.length() == 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, append))) {
            if (writeHeader) {
                pw.println(TaskMetric.getCsvHeader());
            }
            for (TaskMetric m : metrics) {
                pw.println(m.toCsvRow());
            }
        } catch (IOException e) {
            System.err.println("Error writing task_results.csv: " + e.getMessage());
        }
    }

    public synchronized void writeMigrationLogs(List<MigrationEvent> events, boolean append) {
        File file = new File(outputDir, "migration_log.csv");
        boolean writeHeader = !file.exists() || !append || file.length() == 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, append))) {
            if (writeHeader) {
                pw.println(MigrationEvent.getCsvHeader());
            }
            for (MigrationEvent m : events) {
                pw.println(m.toCsvRow());
            }
        } catch (IOException e) {
            System.err.println("Error writing migration_log.csv: " + e.getMessage());
        }
    }

    public synchronized void writeSummaryRow(String summaryRow, boolean append) {
        File file = new File(outputDir, "experiment_summary.csv");
        boolean writeHeader = !file.exists() || !append || file.length() == 0;
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, append))) {
            if (writeHeader) {
                pw.println("scenario,run_id,algorithm,avg_cpu,peak_cpu,avg_mem,avg_response_time,median_response_time,p95_response_time,completed_tasks,throughput,sla_violations,sla_violation_rate,migrations_count,makespan");
            }
            pw.println(summaryRow);
        } catch (IOException e) {
            System.err.println("Error writing experiment_summary.csv: " + e.getMessage());
        }
    }

    public void clearResultsDirectory() {
        File dir = new File(outputDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".csv")) {
                        f.delete();
                    }
                }
            }
        }
    }
}
