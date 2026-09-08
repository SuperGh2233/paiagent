# understand-anything-viewer

Standalone read-only viewer for [Understand-Anything](https://github.com/Egonex-AI/Understand-Anything) knowledge graphs. Opens the full interactive dashboard for a graph that was already generated with `/understand` — no Claude Code, no LLM, no API key. Only Node.js (>= 18) is required.

## Usage

Run the tarball attached to each GitHub release directly (no npm registry involved):

```bash
npx https://github.com/Egonex-AI/Understand-Anything/releases/latest/download/understand-anything-viewer.tgz /path/to/analyzed/project
```

The project directory (default: current directory) must contain a data directory — `.ua/` or legacy `.understand-anything/` — with a `knowledge-graph.json`. The terminal prints a tokenized URL (`http://127.0.0.1:<port>/?token=…`) and opens it in your browser.

Options: `--port <n>` (default 5173, auto-increments if taken), `--no-open`.

Everything is served read-only from local disk, bound to `127.0.0.1`, and gated behind a one-time access token — no data leaves your machine.

## Building the tarball (maintainers)

```bash
pnpm --filter understand-anything-viewer pack:release
gh release upload <tag> understand-anything-plugin/packages/viewer/understand-anything-viewer-*.tgz
```

The pack step builds the dashboard and embeds its compiled `dist/` into the package, producing a fully self-contained, zero-dependency tarball.

---

## 在本仓库内直接使用（PaiAgent 内置）

本目录已随仓库提交，`tools/viewer/` 自带完整产品级 UI（`dist/`）与零依赖启动脚本：

- **Windows**：双击或执行 `tools/viewer/start-dashboard.bat`（自动找 Node，默认项目根为仓库根）
- **macOS/Linux**：`bash tools/viewer/start-dashboard.sh`

两张启动脚本都会打印带访问令牌的 `🔑 Dashboard URL`，并自动打开浏览器；页面访问 `.ua/knowledge-graph.json`，仅绑定 `127.0.0.1`，阅读只读。

> 仓库根目录的 `.ua/knowledge-graph.json` 是本项目（PaiAgent）的分析结果，与 `dist/` 中随版本附带的示例图谱无关——示例图谱已从提交中剔除。
