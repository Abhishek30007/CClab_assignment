# Cloud Resource Allocation: Static vs Dynamic Using CloudSim Plus

Academic project and comparative benchmark evaluating **Static Resource Allocation** vs **Dynamic CPU-based Resource Allocation with Whole-VM Live Migration** under fluctuating multi-tenant cloud workloads.

## System Architecture

- **Simulation Toolkit**: CloudSim Plus 8.5.7
- **Hosts**: 5 Physical Hosts, 8 physical cores @ 2,500 MIPS (20,000 MIPS total), 32 GB RAM, 10 Gbps Bandwidth
- **Virtual Machines**: 20 Multi-core VMs (2 vCPUs, 4 GB RAM), initial Round-Robin placement (4 VMs/Host)
- **Workloads**: Configurable task generator supporting E1 (Low/Stable), E2 (Ramping), E3 (Peak), E4 (Peak+Recovery), E5 (Mixed/Stochastic), and CONTROLLED_HOTSPOT
- **Dynamic Allocation**: Real-time overload detection (CPU $\ge 80\%$) with automated whole-VM migration to feasible destination hosts
- **Dashboard**: Python 3 (Streamlit, Plotly, Pandas) with interactive KPI metrics, timelines, distribution box plots, and audit logs

---

## Quick Start Instructions

### 1. Build the Java Simulation Project
Ensure Java 17+ is installed.
```powershell
.\apache-maven-3.9.9\bin\mvn.cmd clean compile
```

### 2. Generate Dependency Classpath File
```powershell
.\apache-maven-3.9.9\bin\mvn.cmd dependency:build-classpath -Dmdep.outputFile=classpath.txt
```

### 3. Run Controlled Deterministic VM Migration Test
Executes a targeted hotspot scenario on Host 0's VMs to demonstrate real-time whole-VM live migration:
```powershell
java -cp "target/classes;$(Get-Content classpath.txt)" com.cloudallocation.Main test
```

### 4. Run the Full Benchmark Suite (E1 through E5 + Hotspot)
```powershell
# Quick run (1 repetition per scenario):
java -cp "target/classes;$(Get-Content classpath.txt)" com.cloudallocation.Main quick

# Standard comprehensive benchmark (3 repetitions per scenario):
java -cp "target/classes;$(Get-Content classpath.txt)" com.cloudallocation.Main
```

### 5. Launch the Interactive Analytics Dashboard
```powershell
streamlit run dashboard/app.py
```
Open your browser at `http://localhost:8501`.

---

## Directory Structure

```
c:/Users/BIT/CCtask/
├── pom.xml                               # Maven project configuration
├── config/
│   └── simulation.properties            # Centralized simulation configuration
├── src/
│   ├── main/java/com/cloudallocation/
│   │   ├── Main.java                     # CLI execution runner
│   │   ├── CloudConfiguration.java       # Properties loader
│   │   ├── HostFactory.java              # Physical host factory
│   │   ├── VMFactory.java                # Virtual machine factory
│   │   ├── VmSchedulerTimeSharedOvercommit.java # Custom hypervisor vCPU overcommit
│   │   ├── WorkloadGenerator.java        # Scenarios E1-E5 & Hotspot
│   │   ├── StaticAllocationPolicy.java   # Baseline static placement
│   │   ├── DynamicAllocationPolicy.java  # Overload detector & Whole-VM migration
│   │   ├── MetricsCollector.java         # Intervals, latency & SLA tracking
│   │   ├── CSVExporter.java              # CSV export engine
│   │   ├── ExperimentRunner.java         # Multi-scenario benchmark coordinator
│   │   └── models/                       # Data models (IntervalMetric, TaskMetric, MigrationEvent)
│   └── test/java/com/cloudallocation/    # JUnit test suite
├── results/                              # Generated CSV benchmark datasets
│   ├── static_results.csv
│   ├── dynamic_results.csv
│   ├── combined_results.csv
│   ├── task_results.csv
│   ├── migration_log.csv
│   └── experiment_summary.csv
├── dashboard/
│   ├── app.py                            # Streamlit & Plotly interactive app
│   └── requirements.txt                  # Python dependencies
├── reports/
│   └── academic_report.md                # Full academic research paper
└── docs/
    └── architecture.md                   # System design & mathematical formulas
```
