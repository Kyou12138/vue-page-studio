# VuePage Studio

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](./backend)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen.svg)](./frontend)

**简体中文** | [English](./README.en.md)

面向 **Vue 中后台页面** 的 **低代码 + AI** 工坊。

描述页面（或加载示例）→ L2 画布编辑结构 → 调属性 → 预览 → 导出可维护的 **Vue 3 SFC**。  
以 **PageDSL（JSON）** 为真相源：AI 只读写 DSL，Vue 由后端 **确定性导出**。

| | |
|--|--|
| **后端** | Spring Boot 3 · LangChain4j · OpenAI 兼容 API |
| **前端** | Vue 3 · TypeScript · Vite · Pinia · Element Plus |

---

## 功能

- AI 生成 / 修改 PageDSL  
- 21 种画布组件  
- L2 画布：投放、排序、Container 堆叠 / 双列  
- 属性面板 + 撤销重做  
- 预览 + 后端导出  
- **无 API Key 也可**加载示例、编辑、导出  

---

## 目录

```text
vue-page-studio/
├── README.md          # 中文（默认）
├── README.en.md       # English
├── LICENSE
├── shared/page-dsl.schema.json
├── fixtures/dsl/
├── docs/manual-test.md
├── backend/
└── frontend/
```

---

## 环境

- JDK 17+
- Maven 3.9+
- Node.js 20+
- 可选：OpenAI 兼容 API Key  

---

## 快速开始

### 后端

```bash
export STUDIO_LLM_API_KEY=sk-...   # 可选
cd backend
mvn spring-boot:run
```

Windows 若 8080 被占用：

```powershell
$env:STUDIO_LLM_API_KEY = "sk-..."
cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8088"
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

浏览器打开：**http://127.0.0.1:3000**

代理端口见 `frontend/vite.config.ts`，需与后端一致。

### 无 API Key 时

1. 启动后端 + 前端  
2. 点击 **加载示例**  
3. 拖组件、改属性，查看 **页面预览 / Vue 代码**  

---

## 环境变量

| 变量 | 说明 | 默认 |
|------|------|------|
| `STUDIO_LLM_BASE_URL` | API Base URL | `https://api.openai.com/v1` |
| `STUDIO_LLM_API_KEY` | API Key | 空 |
| `STUDIO_LLM_MODEL` | 模型名 | `gpt-4o-mini` |

---

## API

| 方法 | 路径 | 作用 |
|------|------|------|
| `POST` | `/api/ai/generate` | 描述 → PageDSL |
| `POST` | `/api/ai/modify` | DSL + 指令 → PageDSL |
| `POST` | `/api/export/vue` | PageDSL → Vue SFC |
| `POST` | `/api/dsl/validate` | 校验 |

契约：[`shared/page-dsl.schema.json`](./shared/page-dsl.schema.json)

---

## 组件库（21 种）

PageHeader、Breadcrumb、AlertBanner、SearchBar、DataTable、Pagination、FormSection、DescriptionList、ActionBar、StatCards、Tabs、Steps、Timeline、TagGroup、TreeNav、EmptyState、ResultBlock、ImageBlock、Divider、TextBlock、Container。

---

## 测试

```bash
cd backend && mvn test
cd frontend && npm test && npm run build
```

手工清单：[`docs/manual-test.md`](./docs/manual-test.md)

---

## 许可证

[MIT](./LICENSE)
