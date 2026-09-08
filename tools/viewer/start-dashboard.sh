#!/usr/bin/env bash
# PaiAgent 知识图谱仪表盘一键启动（跨平台：Windows 上也可行——内部会调用 node）
# 用法:  bash tools/viewer/start-dashboard.sh   （在仓库根目录执行，或传项目目录作参数）
#
# 依赖:  Node.js >= 18（无需任何 npm 安装）
# 说明:  只读打开 .ua/knowledge-graph.json 的交互式仪表盘，一键式 token 校验，仅绑定 127.0.0.1。

set -euo pipefail

# ── 定位项目根（本脚本所在目录的上一级） ──────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${1:-$(dirname "$SCRIPT_DIR")}"

# ── 定位 viewer 入口 ──────────────────────────────────────────────────
VIEWER="$SCRIPT_DIR/bin/viewer.mjs"

# ── 检查必备文件 ──────────────────────────────────────────────────────
if [ ! -f "$VIEWER" ]; then
  echo "错误：找不到 $VIEWER （本仓库的 tools/viewer/ 结构不完整？）" >&2
  exit 1
fi
if [ ! -f "$PROJECT_ROOT/.ua/knowledge-graph.json" ] && [ ! -f "$PROJECT_ROOT/.understand-anything/knowledge-graph.json" ]; then
  echo "错误：未找到知识图谱（缺少 $PROJECT_ROOT/.ua/knowledge-graph.json）。" >&2
  echo "      请先运行 /understand 生成图谱，再执行本脚本。" >&2
  exit 1
fi

# ── 选择 Node 启动器（Windows 下优先 cmd 直调以获得更好退出行为） ──────
if command -v node >/dev/null 2>&1; then
  NODE_CMD="node"
elif [ -n "${PROGRAMFILES:-}" ] && [ -f "$PROGRAMFILES/nodejs/node.exe" ]; then
  NODE_CMD="$PROGRAMFILES/nodejs/node.exe"
else
  echo "错误：未找到 Node.js（需要 >= 18）。请先安装 Node.js。" >&2
  exit 1
fi

# ── 启动服务（前台，Ctrl+C 停止） ─────────────────────────────────────
echo "启动 PaiAgent 知识图谱仪表盘…"
echo "图谱来源：$PROJECT_ROOT/.ua/knowledge-graph.json"
echo "访问令牌：每次启动随机生成（终端里打印的 URL 已内嵌 token）"
echo "按 Ctrl+C 停止服务。"
echo ""
exec "$NODE_CMD" "$VIEWER" "$PROJECT_ROOT" "$@"