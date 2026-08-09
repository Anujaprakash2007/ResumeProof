@echo off
echo =================================================
echo   STARTING RESUMEPROOF RECRUITMENT PLATFORM
echo =================================================

where javac >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Java compiler detected. Compiling Java core...
    call build.bat
    java -cp bin com.resumeproof.Main
) else (
    echo [INFO] Node.js runtime detected. Starting zero-dependency ResumeProof server...
    node server.js
)
