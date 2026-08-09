@echo off
echo =================================================
echo   COMPILING RESUMEPROOF JAVA CORE (ZERO DEPENDENCIES)
echo =================================================

if not exist bin mkdir bin

javac -d bin -sourcepath src src/com/resumeproof/Main.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation finished cleanly with 0 errors.
) else (
    echo [ERROR] Compilation failed.
)
