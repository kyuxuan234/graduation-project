@echo off
chcp 65001 >nul
cd /d "%~dp0"

docker compose down
if errorlevel 1 exit /b 1
echo 服务已停止，数据已保留。如需彻底清理请执行：docker compose down -v
