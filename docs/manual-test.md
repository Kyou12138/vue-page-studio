# VuePage Studio 手工验收清单

依据设计规格（`docs/superpowers/specs/2026-07-10-vue-page-studio-design.md`）与实现计划 Task 12。

**前置**：后端 `http://localhost:8080`、前端 `http://localhost:5173` 均已启动。

| # | 场景 | 操作步骤 | 期望结果 | 通过 |
|---|------|----------|----------|------|
| 1 | 加载示例 → 搜索+表+分页 | 打开工坊 → 点击 **加载示例**（画布空状态或 AI 面板） | DSL 载入「用户管理列表」；画布出现 PageHeader / SearchForm / DataTable / Pagination 等区块；底栏 **页面预览** 可见搜索区、表格列、分页 | ☐ |
| 2 | 画布投放组件 | 从左侧组件库拖一个组件（如 `StatCard`）到中栏画布空白或某卡片上/下半区 | 画布新增对应卡片；预览同步出现；可撤销一步 | ☐ |
| 3 | 画布调序 | 抓住卡片右侧 **⋮⋮** 手柄，上下拖动改变顺序 | 画布顺序变化并写回 DSL；预览顺序一致；可撤销 | ☐ |
| 4 | 不调 AI 改列 | 选中 `DataTable` → 右侧属性面板修改列 `label` / 增删列 → 失焦或确认 | **不发起** AI 请求；预览列名/列数立即更新；一步 history | ☐ |
| 5 | 风格切换 plain | 顶栏风格选 **Plain** | `pageDsl.style` 变为 `plain`；预览切换为 plain 分支；不调 AI | ☐ |
| 6 | 导出（需后端） | 顶栏 **导出 Vue** 或底栏 **Vue 代码** Tab / **复制代码** | 请求 `POST /api/export/vue` 成功；下载/展示 SFC；`style=plain` 时模板中 **无** `el-table` 等 Element 组件标签 | ☐ |
| 7 | 无 Key 错误提示 | 未配置 `STUDIO_LLM_API_KEY` 时，在 AI 面板点 **生成** 或 **修改** | 界面出现清晰错误（含 `CONFIG_MISSING` 或「请配置后端环境变量 STUDIO_LLM_API_KEY」类文案）；画布/导出仍可用 | ☐ |
| 8 | （可选）AI 加列保留旧列 | 配置有效 Key → 加载示例 → 对话「给表格加一列状态」 | 修改成功；原有列 id/字段尽量保留（IdReconciler）；预览多一列 | ☐ |
| 9 | （可选）危险标题转义 | 属性或 DSL 将 `title` 设为含 `<script>` / `"` 等字符 → 导出 | 导出 SFC 中标题已按 HTML/属性上下文转义，无裸注入片段（与后端 `HtmlEscaper` 单测一致） | ☐ |
| 10 | Container 内部投放 | 从组件库拖入 `Container` → 属性 layout=stack → 再拖组件到「内部 · stack」区域 | 子节点出现在 Container 内；可排序；预览可见分组 | ☐ |
| 11 | two-column 列投放 | Container layout=two-column → 分别向左列/右列区域拖入组件 | 左右列各自有子节点；预览两列布局 | ☐ |

## 自动化对照

手工前建议先跑通：

```powershell
# 后端
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
cd vue-page-studio\backend
mvn -q test

# 前端
cd vue-page-studio\frontend
npm test
npm run build
```

期望：后端全部测试 PASS；前端 vitest PASS；`npm run build` 成功。
