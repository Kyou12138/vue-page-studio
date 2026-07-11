# VuePage Studio

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](./backend)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen.svg)](./frontend)

[简体中文](./README.md) | **English**

Low-code **+** AI workshop for **Vue mid-admin pages**.

Describe a page (or load a sample) → edit structure on an L2 canvas → tweak props → preview → export a maintainable **Vue 3 SFC**.  
The page is stored as **PageDSL** (JSON). AI only reads/writes DSL; Vue code is produced by a **deterministic exporter**.

| | |
|--|--|
| **Backend** | Spring Boot 3 · LangChain4j · OpenAI-compatible API |
| **Frontend** | Vue 3 · TypeScript · Vite · Pinia · Element Plus |

---

## Features

- **AI generate / modify** PageDSL via OpenAI-compatible models  
- **21 canvas components** (tables, forms, steps, tree, empty state, …)  
- **L2 canvas**: palette drop, reorder, Container stack / two-column slots  
- **Property panel** + undo/redo  
- **Preview** + **backend export** (`element-plus` or `plain`)  
- **Works without an API key** (load fixtures, edit canvas, export)

---

## Layout

```text
vue-page-studio/
├── README.md          # Chinese (default on GitHub)
├── README.en.md       # English
├── LICENSE
├── shared/page-dsl.schema.json
├── fixtures/dsl/
├── docs/manual-test.md
├── backend/
└── frontend/
```

---

## Requirements

- JDK **17+**
- Maven **3.9+**
- Node.js **20+**
- Optional: OpenAI-compatible API key for AI features

---

## Quick start

### Backend (default port `8080`)

```bash
export STUDIO_LLM_API_KEY=sk-...   # optional
cd backend
mvn spring-boot:run
```

Windows if port 8080 is busy:

```powershell
$env:STUDIO_LLM_API_KEY = "sk-..."
cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8088"
```

### Frontend (dev port `3000` in this project)

```bash
cd frontend
npm install
npm run dev
```

Open **http://127.0.0.1:3000**.

Align `frontend/vite.config.ts` proxy target with your backend port.

### Without an API key

1. Start backend + frontend  
2. Click **Load sample**  
3. Drag components, edit props, open **Preview** / **Vue code**  

---

## Environment variables

| Variable | Description | Default |
|----------|-------------|---------|
| `STUDIO_LLM_BASE_URL` | OpenAI-compatible base URL | `https://api.openai.com/v1` |
| `STUDIO_LLM_API_KEY` | API key | _(empty)_ |
| `STUDIO_LLM_MODEL` | Model name | `gpt-4o-mini` |

---

## API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/ai/generate` | description → PageDSL |
| `POST` | `/api/ai/modify` | DSL + instruction → PageDSL |
| `POST` | `/api/export/vue` | PageDSL → Vue SFC |
| `POST` | `/api/dsl/validate` | validate PageDSL |

Schema: [`shared/page-dsl.schema.json`](./shared/page-dsl.schema.json)

---

## Components (21)

PageHeader, Breadcrumb, AlertBanner, SearchBar, DataTable, Pagination, FormSection, DescriptionList, ActionBar, StatCards, Tabs, Steps, Timeline, TagGroup, TreeNav, EmptyState, ResultBlock, ImageBlock, Divider, TextBlock, Container.

---

## Tests

```bash
cd backend && mvn test
cd frontend && npm test && npm run build
```

Manual checklist: [`docs/manual-test.md`](./docs/manual-test.md)

---

## License

[MIT](./LICENSE)
