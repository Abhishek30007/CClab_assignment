@echo off
REM ========================================================
REM Runner for CloudSim: Static vs Dynamic Resource Allocation
REM ========================================================

echo ========================================================
echo [1/3] Compiling CloudSim Simulation Project...
echo ========================================================
call apache-maven-3.9.9\bin\mvn.cmd -q clean compile
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo [2/3] Executing Academic Benchmark Simulation (E1-E5)...
echo ========================================================
call apache-maven-3.9.9\bin\mvn.cmd -q dependency:build-classpath -Dmdep.outputFile=classpath.txt
set /p CP=<classpath.txt
java -cp "target\classes;%CP%" com.cloudallocation.Main quick
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Simulation execution failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ========================================================
echo [3/3] Launching Streamlit Interactive Dashboard...
echo ========================================================
echo Opening browser at http://localhost:8501 ...
start "" "http://localhost:8501"
streamlit run dashboard\app.py
pause
