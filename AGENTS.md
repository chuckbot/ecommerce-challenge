# AI Agent Context: Enterprise E-Commerce Challenge

## 1. Project Overview
This is a corporate coding challenge for a Full-Stack E-Commerce application. The goal is not just to make it work, but to demonstrate senior-level architectural decisions, robust data handling, and production-ready code.

### Strict Global Rules
- **NO AI COMMENTS:** The challenge explicitly states: *"AI is allowed, if you use it, please remove comments from the code."* Do not output any inline comments (`;;`) in Clojure/ClojureScript files. Use clean code and docstrings instead.
- **Language Stack:** Backend in Clojure. Frontend in ClojureScript.
- **Architecture:** Hexagonal Architecture (Ports and Adapters).
- **Execution:** Must be runnable entirely via `docker compose up --build`.

## 2. Tech Stack
- **Backend:** Clojure, Integrant (State/Lifecycle), Ring/Jetty (HTTP), Reitit (Routing + Swagger), Malli (Validation/Coercion).
- **Database:** PostgreSQL 16, next.jdbc, HoneySQL 2, HikariCP, Migratus (Migrations).
- **Frontend:** ClojureScript, Shadow-cljs, Reagent (React), re-frame (State Management), TailwindCSS, cljs-ajax.
- **Domain:** `clojure.data.csv` for parsing, with shared `Malli` schemas in `.cljc` files.

## 3. Current Progress
- Scaffolding complete.
- **Database:** Migrations (`001-init.up.sql` & `001-init.down.sql`) configured with Migratus `--;;` separators and strict `CHECK` constraints. `db.core`, `db.products` (full CRUD + search + upsert), and `db.orders` (atomic purchase with `SELECT ... FOR UPDATE`) implemented.
- **Domain:** Pure CSV parser (`domain/csv_import.clj`) and Malli schemas (`schema.cljc`) built to detect anomalies line-by-line, deduplicate in-batch SKUs, and return structured audit reports.
- **HTTP:** Reitit router configured with Multipart upload support, Malli coercion, Swagger UI (`/api/docs`), and static SPA serving (`/`).
- **Frontend:** Complete ClojureScript/Reagent/re-frame SPA with Catalog CRUD modal, hybrid search & category filters, Drag & Drop CSV import with detailed anomaly report, and Cart/Checkout + Order history.
- **Docker & Build:** `build.clj`, `Dockerfile`, and `docker-compose.yml` configured for end-to-end execution via `docker compose up --build`.