@echo off
chcp 65001 >nul
cd /d "%~dp0"

docker --version >nul 2>&1
if errorlevel 1 (
  echo 请先安装 Docker Desktop
  exit /b 1
)

if not exist ".env" (
  echo 请复制 .env.example 为 .env 并填写配置
  exit /b 1
)

docker compose up -d --build
if errorlevel 1 exit /b 1

timeout /t 20 /nobreak >nul
echo 前端 http://localhost:8080
echo 后端 http://localhost:9090
docker compose ps
