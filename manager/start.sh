#!/bin/bash
set -e
cd "$(dirname "$0")"

if ! docker --version >/dev/null 2>&1; then
  echo "请先安装 Docker Desktop"
  exit 1
fi

if [ ! -f .env ]; then
  echo "请复制 .env.example 为 .env 并填写配置"
  exit 1
fi

docker compose up -d --build
sleep 20
echo "前端 http://localhost:8080"
echo "后端 http://localhost:9090"
docker compose ps
