# Boardgame Tracker Frontend

Frontend приложение для Boardgame Tracker (Vite + React + TypeScript + Mantine + React Query + Orval).

## Prerequisites

- Node.js `>=20.19.0`
- npm
- Backend Boardgame Tracker, запущенный локально

## Setup

```bash
npm install
cp .env.dist .env.local
```

## Environment Variables

Используется только один обязательный env:

- `VITE_API_BASE_URL`

Рекомендованные значения:

- backend через `./gradlew bootRun`: `VITE_API_BASE_URL=http://localhost:8080`
- backend через `docker compose` app profile: `VITE_API_BASE_URL=http://localhost:8088`

## Commands

```bash
npm run dev
npm run api:generate
npm run typecheck
npm run lint
npm run build
```

## API Client Generation

Typed client генерируется из backend OpenAPI:

- source of truth: `../src/main/resources/openapi/openapi.yaml`
- output: `src/shared/api/generated/`

Команда:

```bash
npm run api:generate
```

Перед генерацией выполняется pre-step `scripts/prepare-openapi-for-orval.mjs`, который готовит dereferenced OpenAPI для Orval.

## Local Run

1. Убедиться, что backend запущен на URL из `VITE_API_BASE_URL`.
2. Запустить frontend:

```bash
npm run dev -- --host 127.0.0.1
```

3. Открыть URL из вывода Vite (обычно `http://127.0.0.1:5173`).

## Demo Checklist

- Авторизация: register/login/logout.
- Dashboard stats и quick actions.
- Boardgames search + add existing game.
- Add custom game.
- Collection list + edit/delete + quick `+1 play`.
- Collection item details + play sessions CRUD.
- Shelf of shame + quick `+1 play`.
- Tonight recommendations + open collection item details.
