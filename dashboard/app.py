import streamlit as st
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go
from pathlib import Path
import os

# Page Configuration
st.set_page_config(
    page_title="CloudSim: Static vs Dynamic Resource Allocation",
    page_icon="☁️",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom CSS for polished aesthetics
st.markdown("""
<style>
    .main-header {
        font-size: 2.2rem;
        font-weight: 700;
        background: linear-gradient(120deg, #1e3c72 0%, #2a5298 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        margin-bottom: 0.2rem;
    }
    .sub-header {
        font-size: 1.05rem;
        color: #555;
        margin-bottom: 1.5rem;
    }
    .metric-card {
        background-color: #f8f9fa;
        border-radius: 8px;
        padding: 16px;
        border-left: 5px solid #2a5298;
        box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    }
    .card-title {
        font-size: 0.85rem;
        text-transform: uppercase;
        color: #6c757d;
        font-weight: 600;
    }
    .card-value {
        font-size: 1.8rem;
        font-weight: bold;
        color: #212529;
    }
    .card-delta {
        font-size: 0.85rem;
        margin-top: 4px;
    }
</style>
""", unsafe_allow_html=True)

# File Paths
RESULTS_DIR = Path(__file__).resolve().parent.parent / "results"
COMBINED_FILE = RESULTS_DIR / "combined_results.csv"
TASK_FILE = RESULTS_DIR / "task_results.csv"
MIGRATION_FILE = RESULTS_DIR / "migration_log.csv"
SUMMARY_FILE = RESULTS_DIR / "experiment_summary.csv"

@st.cache_data
def load_data():
    if not RESULTS_DIR.exists():
        return None, None, None, None
    
    combined_df = pd.read_csv(COMBINED_FILE) if COMBINED_FILE.exists() and COMBINED_FILE.stat().st_size > 0 else pd.DataFrame()
    task_df = pd.read_csv(TASK_FILE) if TASK_FILE.exists() and TASK_FILE.stat().st_size > 0 else pd.DataFrame()
    migration_df = pd.read_csv(MIGRATION_FILE) if MIGRATION_FILE.exists() and MIGRATION_FILE.stat().st_size > 0 else pd.DataFrame()
    summary_df = pd.read_csv(SUMMARY_FILE) if SUMMARY_FILE.exists() and SUMMARY_FILE.stat().st_size > 0 else pd.DataFrame()

    return combined_df, task_df, migration_df, summary_df

# Title Section
st.markdown('<div class="main-header">Comparison of Static and Dynamic Resource Allocation in Cloud Computing</div>', unsafe_allow_html=True)
st.markdown('<div class="sub-header">Simulation Analysis & Benchmarking Platform using CloudSim Plus | 5 Physical Hosts • 20 Virtual Machines</div>', unsafe_allow_html=True)

combined_df, task_df, migration_df, summary_df = load_data()

if combined_df.empty and summary_df.empty:
    st.error("⚠️ No simulation results found in `results/` directory.")
    st.info("Run the simulation first using:\n```powershell\njava -cp \"target/classes;$(Get-Content classpath.txt)\" com.cloudallocation.Main\n```")
    st.stop()

# ================= SIDEBAR CONTROLS =================
st.sidebar.title("🎮 Experiment Controls")

# Scenario Selector
scenarios = ["All"]
if not summary_df.empty and "scenario" in summary_df.columns:
    scenarios.extend(summary_df["scenario"].dropna().unique().tolist())
elif not combined_df.empty and "experiment_id" in combined_df.columns:
    scenarios.extend(combined_df["experiment_id"].dropna().unique().tolist())

selected_scenario = st.sidebar.selectbox("Select Scenario", scenarios, index=0)

# Algorithm Selector
algorithms = st.sidebar.radio("Allocation Strategy", ["Both (Comparative)", "Static Allocation", "Dynamic Allocation (with Migration)"], index=0)

# Time Range Slider
min_time, max_time = 0.0, 1200.0
if not combined_df.empty and "time" in combined_df.columns:
    min_time = float(combined_df["time"].min())
    max_time = float(combined_df["time"].max())

time_range = st.sidebar.slider("Simulation Time Range (s)", min_value=0.0, max_value=max(1200.0, max_time), value=(0.0, max(1200.0, max_time)), step=10.0)

# Data Filtering
filtered_combined = combined_df.copy()
filtered_task = task_df.copy()
filtered_summary = summary_df.copy()
filtered_migrations = migration_df.copy()

if selected_scenario != "All":
    if not filtered_combined.empty and "experiment_id" in filtered_combined.columns:
        filtered_combined = filtered_combined[filtered_combined["experiment_id"] == selected_scenario]
    if not filtered_task.empty and "experiment_id" in filtered_task.columns:
        filtered_task = filtered_task[filtered_task["experiment_id"] == selected_scenario]
    if not filtered_summary.empty and "scenario" in filtered_summary.columns:
        filtered_summary = filtered_summary[filtered_summary["scenario"] == selected_scenario]

if algorithms == "Static Allocation":
    if not filtered_combined.empty:
        filtered_combined = filtered_combined[filtered_combined["algorithm"] == "Static"]
    if not filtered_task.empty:
        filtered_task = filtered_task[filtered_task["algorithm"] == "Static"]
    if not filtered_summary.empty:
        filtered_summary = filtered_summary[filtered_summary["algorithm"] == "Static"]
elif algorithms == "Dynamic Allocation (with Migration)":
    if not filtered_combined.empty:
        filtered_combined = filtered_combined[filtered_combined["algorithm"] == "Dynamic"]
    if not filtered_task.empty:
        filtered_task = filtered_task[filtered_task["algorithm"] == "Dynamic"]
    if not filtered_summary.empty:
        filtered_summary = filtered_summary[filtered_summary["algorithm"] == "Dynamic"]

if not filtered_combined.empty and "time" in filtered_combined.columns:
    filtered_combined = filtered_combined[(filtered_combined["time"] >= time_range[0]) & (filtered_combined["time"] <= time_range[1])]

# Download Section in Sidebar
st.sidebar.markdown("---")
st.sidebar.subheader("📥 Export Datasets")
if COMBINED_FILE.exists():
    with open(COMBINED_FILE, "rb") as f:
        st.sidebar.download_button("Download combined_results.csv", f, file_name="combined_results.csv", mime="text/csv")
if MIGRATION_FILE.exists():
    with open(MIGRATION_FILE, "rb") as f:
        st.sidebar.download_button("Download migration_log.csv", f, file_name="migration_log.csv", mime="text/csv")

# ================= TABS NAVIGATION =================
tab1, tab2, tab3, tab4, tab5, tab6, tab7 = st.tabs([
    "📊 Executive Overview",
    "⚡ CPU & Host Load",
    "🧠 Memory Analysis",
    "⏱️ Response Time & SLA",
    "🚀 Throughput",
    "🔄 Whole-VM Migration",
    "📋 Summary Tables"
])

# ================= TAB 1: EXECUTIVE OVERVIEW =================
with tab1:
    st.subheader("Key Performance Indicators (KPIs)")
    
    # Calculate baseline comparison metrics
    stat_summary = summary_df[summary_df["algorithm"] == "Static"] if not summary_df.empty else pd.DataFrame()
    dyn_summary = summary_df[summary_df["algorithm"] == "Dynamic"] if not summary_df.empty else pd.DataFrame()

    if selected_scenario != "All":
        stat_summary = stat_summary[stat_summary["scenario"] == selected_scenario]
        dyn_summary = dyn_summary[dyn_summary["scenario"] == selected_scenario]

    stat_cpu = stat_summary["avg_cpu"].mean() * 100 if not stat_summary.empty else 0.0
    dyn_cpu = dyn_summary["avg_cpu"].mean() * 100 if not dyn_summary.empty else 0.0

    stat_peak = stat_summary["peak_cpu"].max() * 100 if not stat_summary.empty else 0.0
    dyn_peak = dyn_summary["peak_cpu"].max() * 100 if not dyn_summary.empty else 0.0

    stat_resp = stat_summary["avg_response_time"].mean() if not stat_summary.empty else 0.0
    dyn_resp = dyn_summary["avg_response_time"].mean() if not dyn_summary.empty else 0.0

    stat_sla = stat_summary["sla_violations"].sum() if not stat_summary.empty else 0
    dyn_sla = dyn_summary["sla_violations"].sum() if not dyn_summary.empty else 0

    total_migrations = dyn_summary["migrations_count"].sum() if not dyn_summary.empty else len(migration_df)

    kpi1, kpi2, kpi3, kpi4, kpi5, kpi6 = st.columns(6)
    with kpi1:
        st.metric(label="Static Avg CPU", value=f"{stat_cpu:.1f}%")
        st.caption(f"Peak: {stat_peak:.1f}%")
    with kpi2:
        st.metric(label="Dynamic Avg CPU", value=f"{dyn_cpu:.1f}%", delta=f"{dyn_cpu - stat_cpu:.1f}%")
        st.caption(f"Peak: {dyn_peak:.1f}%")
    with kpi3:
        st.metric(label="Static Avg Resp Time", value=f"{stat_resp:.1f} s")
    with kpi4:
        st.metric(label="Dynamic Avg Resp Time", value=f"{dyn_resp:.1f} s", delta=f"{dyn_resp - stat_resp:.1f} s", delta_color="inverse")
    with kpi5:
        st.metric(label="SLA Violations", value=f"{dyn_sla}", delta=f"{dyn_sla - stat_sla}", delta_color="inverse")
        st.caption(f"Static: {stat_sla} | Dyn: {dyn_sla}")
    with kpi6:
        st.metric(label="Whole-VM Migrations", value=f"{total_migrations}")
        st.caption("Triggered by Overload (>80%)")

    st.markdown("---")
    
    col_a, col_b = st.columns(2)
    with col_a:
        st.markdown("### Workload vs Host Distribution Context")
        st.info("""
        - **Static Allocation**: Initial placement of 4 VMs per Host using Round-Robin. Initial mapping remains fixed forever. When hot tasks arrive at specific VMs, their hosting physical host experiences sustained 100% saturation and contention.
        - **Dynamic Allocation**: Real-time overload detection triggers every 10 simulation seconds. When Host CPU utilization exceeds the 80% threshold, candidate VMs are selected and whole-VM live migration transfers them to feasible destination hosts with available capacity.
        """)
    with col_b:
        if not summary_df.empty:
            st.markdown("### Scenario-wise Average CPU Utilization")
            fig_bar = px.bar(
                summary_df, 
                x="scenario", 
                y="avg_cpu", 
                color="algorithm", 
                barmode="group",
                labels={"avg_cpu": "Avg CPU Utilization", "scenario": "Scenario"},
                color_discrete_map={"Static": "#4a90e2", "Dynamic": "#50e3c2"}
            )
            fig_bar.update_layout(yaxis_tickformat='.0%', margin=dict(l=20, r=20, t=30, b=20), height=300)
            st.plotly_chart(fig_bar, use_container_width=True)

# ================= TAB 2: CPU & HOST LOAD =================
with tab2:
    st.subheader("CPU Utilization Over Time")
    if not filtered_combined.empty and "time" in filtered_combined.columns:
        fig_cpu = px.line(
            filtered_combined,
            x="time",
            y="avg_cpu",
            color="algorithm",
            line_dash="algorithm",
            title="Cloud-Wide Average CPU Utilization Over Simulation Timeline",
            labels={"avg_cpu": "Average CPU Utilization", "time": "Simulation Time (s)"},
            color_discrete_map={"Static": "#e74c3c", "Dynamic": "#2ecc71"}
        )
        fig_cpu.add_hline(y=0.80, line_dash="dash", line_color="orange", annotation_text="Overload Threshold (80%)")
        fig_cpu.add_hline(y=0.30, line_dash="dot", line_color="blue", annotation_text="Underload Threshold (30%)")
        fig_cpu.update_layout(yaxis_tickformat='.0%', height=400)
        st.plotly_chart(fig_cpu, use_container_width=True)

        col_c1, col_c2 = st.columns(2)
        with col_c1:
            fig_max = px.line(
                filtered_combined,
                x="time",
                y="max_cpu",
                color="algorithm",
                title="Peak Host CPU Utilization (Hotspot Detection)",
                labels={"max_cpu": "Max Host CPU Utilization", "time": "Simulation Time (s)"},
                color_discrete_map={"Static": "#e74c3c", "Dynamic": "#2ecc71"}
            )
            fig_max.add_hline(y=0.80, line_dash="dash", line_color="orange")
            fig_max.update_layout(yaxis_tickformat='.0%', height=350)
            st.plotly_chart(fig_max, use_container_width=True)
        with col_c2:
            st.markdown("### Overload & Migration Impact")
            st.markdown(r"""
            In Cloud computing environments, hotspot hosts cause severe task queue delays.
            - Under **Static Allocation**, peak host CPU frequently hits **100% saturation**, resulting in high execution queuing for all co-located VMs.
            - Under **Dynamic Allocation**, the whole-VM migration mechanism activates within 10 seconds of an overload event ($t > 80\%$), shifting candidate VMs to underloaded hosts and mitigating sustained host contention.
            """)
    else:
        st.warning("No CPU time-series data matching the selected criteria.")

# ================= TAB 3: MEMORY ANALYSIS =================
with tab3:
    st.subheader("Memory Utilization Analysis")
    if not filtered_combined.empty and "avg_memory" in filtered_combined.columns:
        fig_mem = px.line(
            filtered_combined,
            x="time",
            y="avg_memory",
            color="algorithm",
            title="Cloud-Wide Memory Allocation Over Simulation Time",
            labels={"avg_memory": "Memory Utilization Rate", "time": "Simulation Time (s)"},
            color_discrete_map={"Static": "#9b59b6", "Dynamic": "#3498db"}
        )
        fig_mem.update_layout(yaxis_tickformat='.0%', height=400)
        st.plotly_chart(fig_mem, use_container_width=True)

        st.info("""
        **Simulation Note**: In CloudSim Plus, RAM utilization represents provisioned VM RAM against total Host physical RAM (each Host has 32GB, 4 VMs each with 4GB provisioned = 50% baseline allocation). When VMs migrate across hosts, memory reservations transfer seamlessly with the virtual machine.
        """)
    else:
        st.warning("No Memory data available.")

# ================= TAB 4: RESPONSE TIME & SLA =================
with tab4:
    st.subheader("Response Time & SLA Violation Analysis")
    
    col_r1, col_r2 = st.columns(2)
    with col_r1:
        if not filtered_task.empty and "response_time" in filtered_task.columns:
            fig_box = px.box(
                filtered_task,
                x="algorithm",
                y="response_time",
                color="algorithm",
                points="outliers",
                title="Cloudlet Response Time Distribution (s)",
                labels={"response_time": "Response Time (s)", "algorithm": "Policy"},
                color_discrete_map={"Static": "#e74c3c", "Dynamic": "#2ecc71"}
            )
            fig_box.update_layout(height=400)
            st.plotly_chart(fig_box, use_container_width=True)
        else:
            st.warning("No task response time data available.")
            
    with col_r2:
        if not filtered_task.empty and "response_time" in filtered_task.columns:
            fig_hist = px.histogram(
                filtered_task,
                x="response_time",
                color="algorithm",
                barmode="overlay",
                nbins=40,
                title="Response Time Histogram",
                labels={"response_time": "Response Time (s)"},
                color_discrete_map={"Static": "#e74c3c", "Dynamic": "#2ecc71"}
            )
            fig_hist.update_layout(height=400)
            st.plotly_chart(fig_hist, use_container_width=True)

    st.markdown("### SLA Violations Summary")
    if not filtered_summary.empty:
        sla_table = filtered_summary[["scenario", "algorithm", "completed_tasks", "sla_violations", "sla_violation_rate", "avg_response_time", "p95_response_time"]]
        st.dataframe(sla_table, use_container_width=True)

# ================= TAB 5: THROUGHPUT =================
with tab5:
    st.subheader("System Throughput (Completed Tasks / Second)")
    if not filtered_combined.empty and "throughput" in filtered_combined.columns:
        fig_tp = px.line(
            filtered_combined,
            x="time",
            y="throughput",
            color="algorithm",
            title="Instantaneous Throughput Over Time (tasks/s)",
            labels={"throughput": "Throughput (tasks/s)", "time": "Simulation Time (s)"},
            color_discrete_map={"Static": "#e67e22", "Dynamic": "#1abc9c"}
        )
        fig_tp.update_layout(height=400)
        st.plotly_chart(fig_tp, use_container_width=True)
    else:
        st.warning("No throughput records found.")

# ================= TAB 6: WHOLE-VM MIGRATION =================
with tab6:
    st.subheader("Dynamic Whole-VM Migration Events Log")
    st.write("Live migrations executed during overload mitigation when physical host CPU utilization exceeded 80%:")

    if not migration_df.empty:
        col_m1, col_m2 = st.columns([1, 2])
        with col_m1:
            st.markdown("#### Migration Statistics")
            st.metric("Total Successful Migrations", len(migration_df))
            dest_counts = migration_df["destination_host_id"].value_counts().reset_index()
            dest_counts.columns = ["Destination Host", "Migrations Received"]
            st.dataframe(dest_counts, use_container_width=True)
        with col_m2:
            st.markdown("#### Migration Timeline")
            fig_mig = px.scatter(
                migration_df,
                x="timestamp",
                y="vm_id",
                color="source_host_id",
                symbol="destination_host_id",
                title="VM Migration Events (Timestamp vs Migrated VM ID)",
                labels={
                    "timestamp": "Simulation Time (s)",
                    "vm_id": "Migrated VM ID",
                    "source_host_id": "Source Host",
                    "destination_host_id": "Destination Host"
                },
                size=[12]*len(migration_df)
            )
            fig_mig.update_layout(height=350)
            st.plotly_chart(fig_mig, use_container_width=True)

        st.markdown("#### Detailed Migration Audit Log")
        st.dataframe(migration_df, use_container_width=True)
    else:
        st.info("No VM migration events recorded for the current filter.")

# ================= TAB 7: SUMMARY TABLES =================
with tab7:
    st.subheader("Experiment Summary & Benchmark Results")
    if not summary_df.empty:
        st.dataframe(summary_df, use_container_width=True)
    else:
        st.warning("No summary records found.")

    st.markdown("---")
    st.markdown("### Academic Demonstration Guide")
    st.markdown("""
    1. **Baseline Configuration**: 5 Physical Hosts, 20 VMs, 4 VMs/Host initial mapping, Homogeneous 8-core hosts (20,000 MIPS).
    2. **Static Policy**: Initial VM placement pinned throughout 1,200 simulated seconds.
    3. **Dynamic Policy**: Periodic 10s monitoring. When host CPU exceeds 80%, candidate VMs are migrated via CloudSim Plus whole-VM migration to non-overloaded destination hosts.
    4. **Verified Live Migration**: Confirmed in `migration_log.csv` and via CloudSim Plus migration finish listeners.
    """)
