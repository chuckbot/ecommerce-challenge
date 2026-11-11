# Enterprise E-Commerce Platform

> **CSV Dataset Reference:** The example file [`Code Challenge E-Commerce.csv`](./Code%20Challenge%20E-Commerce.csv) was downloaded on **November 10, 2026**.

An enterprise-grade Full-Stack E-Commerce application built with **Clojure**, **ClojureScript**, and **PostgreSQL 16**, following **Hexagonal Architecture (Ports & Adapters)** principles, strict domain schema validation with **Malli**, and ACID transactional guarantees.

---

## 1. How to Run the Project Locally

### Option A: Docker Compose (Recommended — Single Command)

Prerequisites: **Docker** and **Docker Compose**.

```bash
docker compose up --build
```

Once the containers are healthy:
- **Web Application (UI):** [http://localhost:8080](http://localhost:8080)
- **OpenAPI / Swagger UI:** [http://localhost:8080/api/docs](http://localhost:8080/api/docs)
- **PostgreSQL Database (Host Port):** `localhost:5433` (`user: ecommerce`, `password: password123`, `db: ecommerce_db`)

### Option B: Local Development Mode (REPL + Hot Reload)

Prerequisites: **Java 21+**, **Clojure CLI (`deps.edn`)**, **Node.js 20+**, and **Docker** (for PostgreSQL).

1. Start PostgreSQL:
   ```bash
   docker compose up -d db
   ```
2. Install frontend dependencies and build/watch ClojureScript:
   ```bash
   npm install
   npx shadow-cljs release app
   ```
3. Start the Clojure backend server (runs database migrations automatically on startup):
   ```bash
   clojure -M:run
   ```
4. Open [http://localhost:8080](http://localhost:8080).

### Option C: Running Automated Tests (Unit, Integration & Concurrency)

With the PostgreSQL container running (`docker compose up -d db`), execute the complete test suite:

```bash
clojure -M:test
```

The test suite (`test/clj/ecommerce/`) runs **81 assertions across 7 test suites**:
- **Domain & CSV Parser Unit Tests (`ecommerce.domain-test`):** Happy path coercions, currency stripping (`$29.99`), XSS tag rejection, negative stock/price rejection, in-batch SKU deduplication, deterministic order item normalization & `BigDecimal` calculations (`ecommerce.domain.orders`), and exact verification against all 98 lines of `Code Challenge E-Commerce.csv`.
- **Database & HTTP Integration Tests (`ecommerce.integration-test`):** Full Product CRUD lifecycle, Full-Text + `ILIKE` search, duplicate SKU `409 Conflict`, SQL injection immunity, multi-item atomic order calculations, transactional rollback on insufficient stock, and a **10-thread concurrent purchase race-condition test** verifying `SELECT ... FOR UPDATE` never oversells inventory.

---

## 2. Core Features Implemented

1. **Product CRUD (Backend & UI):**
   - Create (`POST /api/products`), Read (`GET /api/products`), Update (`PUT /api/products/:id`), and Delete (`DELETE /api/products/:id`) products with shared client/server Malli validation.
2. **Fault-Tolerant CSV Import (`POST /api/products/import` & `POST /api/products/import-sample`):**
   - Drag-and-drop CSV upload in the UI plus a one-click button to import the bundled `Code Challenge E-Commerce.csv`.
   - Line-by-line parsing (`clojure.data.csv`), currency normalization (`$29.99` -> `29.99`), XSS payload rejection (`<script>`), SQL injection immunity via parameterized queries, blank row skipping (`,,,,,,`), and in-batch SKU deduplication (`RS-001`, `BS-021`) prior to PostgreSQL `ON CONFLICT (sku) DO UPDATE`.
   - Returns a structured audit report detailing imported rows (`85`), merged duplicate SKUs (`3`), skipped blank rows (`2`), and rejected rows (`7`) with line numbers and field-level reasons. See [`DATA_QUALITY_AND_BUGS.md`](./DATA_QUALITY_AND_BUGS.md) for the complete line-by-line breakdown.
3. **Hybrid Product Search (`GET /api/products/search?q=...`):**
   - Combines PostgreSQL GIN-indexed Full-Text Search (`to_tsvector` / `plainto_tsquery`) with `ILIKE` pattern matching across `name`, `sku`, `category`, and `description`, paired with instant UI category filtering.
4. **Atomic Purchase & Checkout (`POST /api/orders` & `GET /api/orders`):**
   - Supports both multi-item Cart Checkout and 1-Click Quick Buy with simulated instant payment (`payment_status = 'COMPLETED'`).
   - Uses `SELECT * FROM products WHERE id = ? FOR UPDATE` inside a database transaction (`next.jdbc/with-transaction`) with deterministic UUID lock ordering to prevent race conditions, overselling, and deadlocks.

---

## 3. Architectural Decisions, Approach & Alternatives Considered

### A. Hexagonal Architecture (Ports & Adapters)
- **Decision:** Separate pure domain logic (`ecommerce.schema`, `ecommerce.domain.csv-import`) from infrastructure adapters (`ecommerce.db.*`, `ecommerce.http.*`) and lifecycle wiring (`integrant` + `aero`).
- **Why:** Allows the CSV parsing, coercion, and business validation rules to remain pure functions without database or HTTP coupling, and enables `.cljc` schema sharing across Clojure (JVM) and ClojureScript (Browser).
- **Alternatives Considered:**
  - *Layered MVC / ActiveRecord style:* Rejected because coupling validation to SQL adapters makes partial-failure batch imports (like dirty CSV processing) brittle and harder to test in isolation.

### B. Database & Concurrency Strategy: PostgreSQL 16 + `SELECT ... FOR UPDATE`
- **Decision:** Use **PostgreSQL 16** with strict DDL `CHECK` constraints (`price >= 0`, `stock >= 0`, `weight_kg >= 0`, `btrim(name) <> ''`), `NUMERIC(12,2)` for exact monetary arithmetic, and pessimistic row-level locking (`SELECT ... FOR UPDATE`) during order placement.
- **Why:** Floating-point types (`DOUBLE PRECISION`) introduce IEEE 754 rounding errors in financial totals, and optimistic application-side stock checks without row locks allow concurrent buyers to oversell inventory into negative stock.
- **Alternatives Considered:**
  - *NoSQL (MongoDB / DynamoDB):* Rejected because multi-item cart checkout and inventory decrements benefit directly from relational ACID transactions and declarative `CHECK` constraints.
  - *Optimistic Locking (`version` column):* Viable, but produces high retry churn on popular SKUs under contention compared to short-lived `SELECT ... FOR UPDATE` locks inside HikariCP pooled connections.

### C. Validation & Coercion: Malli (`.cljc`) over `clojure.spec`
- **Decision:** Use **Malli** (`src/cljc/ecommerce/schema.cljc`) for data-driven schemas, custom string-to-number transformers (`csv-transformer`), and humanized error reporting (`malli.error/humanize`).
- **Why:** Unlike `clojure.spec`, Malli schemas are first-class EDN data structures that integrate natively with Reitit OpenAPI/Swagger generation, support composable transformation pipelines for dirty CSV strings, and produce structured per-field error maps for the UI.

### D. CSV Batch Import Strategy: Partial Success with Audit Report vs. All-or-Nothing Rollback
- **Decision:** Validate row-by-row, isolate malformed rows into an `errors` report with exact line numbers, deduplicate repeated SKUs within the batch (last write wins), and `UPSERT` all valid rows (`ON CONFLICT (sku) DO UPDATE`).
- **Why:** Real-world enterprise catalog feeds frequently contain a handful of dirty rows (e.g., 7 bad rows out of 97 in the challenge dataset). Rejecting the entire file blocks operations; silently dropping bad rows hides data loss. Persisting valid rows idempotently while returning an actionable line-by-line anomaly report gives operators both availability and visibility.

### E. Frontend State Management: ClojureScript + Reagent + Re-frame
- **Decision:** Single-Page Application (SPA) using **Re-frame** event/effect loops (`:http-xhrio`) and reactive subscriptions.
- **Why:** Provides deterministic unidirectional data flow where UI views are pure functions of the application state (`app-db`), automatically synchronizing catalog stock counters, cart badges, and order history after every purchase or CRUD mutation.
