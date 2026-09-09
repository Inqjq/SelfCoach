#!/usr/bin/env bash
# SelfCoach 数据库一键初始化脚本
# 作用：启动 PostgreSQL(pgvector) → 等待就绪 → 建表 → 插默认画像 → 列出表清单
# 用法：在项目根目录执行  bash scripts/init-db.sh
# 可用环境变量覆盖默认值，例如：GOAL=减脂 DAYS=3 bash scripts/init-db.sh

set -euo pipefail

DB_USER="selfcoach"
DB_NAME="selfcoach"
CONTAINER="selfcoach-postgres"
GOAL="${GOAL:-增肌}"
DAYS="${DAYS:-4}"

echo "==> [1/4] 启动 PostgreSQL 容器..."
docker compose up -d postgres

echo "==> [2/4] 等待数据库就绪..."
for i in $(seq 1 30); do
  if docker exec "$CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    echo "    就绪 ✔"
    break
  fi
  sleep 1
  if [ "$i" -eq 30 ]; then
    echo "    启动超时，请查看日志：docker compose logs postgres"
    exit 1
  fi
done

echo "==> [3/4] 执行建表 SQL（db/schema.sql）..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < db/schema.sql

echo "==> [4/4] 插入默认画像（目标=${GOAL}，每周 ${DAYS} 天；仅当表为空时插入）..."
docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c \
  "INSERT INTO user_profile (goal, days_per_week)
   SELECT '${GOAL}', ${DAYS}
   WHERE NOT EXISTS (SELECT 1 FROM user_profile);"

echo "==> 完成！当前 public 下的表："
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" \
  -c "SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename;"