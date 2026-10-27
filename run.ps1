# Runner Script for PowerShell
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "[1/3] Compiling CloudSim Simulation Project..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
.\apache-maven-3.9.9\bin\mvn.cmd clean compile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "[2/3] Executing Academic Benchmark Simulation (E1-E5)..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
.\apache-maven-3.9.9\bin\mvn.cmd dependency:build-classpath -Dmdep.outputFile=classpath.txt
$cp = [IO.File]::ReadAllText("classpath.txt").Trim()
java -cp "target/classes;$cp" com.cloudallocation.Main quick
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "[3/3] Launching Streamlit Interactive Dashboard..." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Start-Process "http://localhost:8501"
streamlit run dashboard/app.py
