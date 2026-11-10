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
- **Database:** Migrations created (`001-init.up.sql`) with strict `CHECK` constraints. `db.core` and `db.products` (upsert logic) implemented.
- **Domain:** Pure CSV parser (`domain/csv_import.clj`) built to detect anomalies line-by-line and return structured reports (`{:valid [...] :invalid [...]}`).
- **HTTP:** Reitit router configured with Multipart upload support and Malli coercion.
- **Frontend:** Basic re-frame setup done, static HTML and Tailwind CSS configured. `shadow-cljs` compiles successfully.

## 4. Current Blocker (Priority #1)
The backend server fails to start due to a database authentication error during Integrant initialization:
`org.postgresql.util.PSQLException: FATAL: password authentication failed for user "ecommerce"`

**Context of the blocker:**
- `docker-compose.yml` sets `POSTGRES_USER: ecommerce`, `POSTGRES_PASSWORD: password123`.
- `config.edn` uses Aero: `:jdbc-url #or [#env DATABASE_URL "jdbc:postgresql://localhost:5432/ecommerce_db?user=ecommerce&password=password123"]`.
- The system host (WSL/Ubuntu) does NOT have a local Postgres running.
- We have already tried `docker compose down -v`, `docker volume rm`, and restarting.
- **Agent Task:** Diagnose and fix the DB connection issue between Integrant/HikariCP and the Dockerized PostgreSQL instance before writing any new features.

## 5. Next Steps (After DB Blocker is Fixed)
1. **Frontend CSV Upload:** Implement the Drag & Drop UI in Reagent to upload the CSV file to the `/api/products/import` endpoint.
2. **Frontend Catalog:** Implement the UI to fetch (`GET /api/products`) and search (`GET /api/products/search?q=...`) the products.
3. **Purchase Flow (Backend):** Implement the `orders` adapter to handle atomic purchases using `SELECT ... FOR UPDATE` to prevent race conditions on stock.
4. **Readme Finalization:** Ensure the `README.md` and `DATA_QUALITY_AND_BUGS.md` detail all the CSV anomalies handled by the system.