@echo off
cd /d "%~dp0hospital_system"
echo Compiling Hospital Management System...
javac *.java
echo Starting PulseCare Web Dashboard on http://localhost:8080 ...
java WebServer
pause
