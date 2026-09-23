#!/bin/bash
set -e
cd "$(dirname "$0")"

docker compose down
echo "服务已停止，数据已保留。如需彻底清理请执行：docker compose down -v"
