# Data Quality Analysis & Anomaly Handling Report

**Dataset Analyzed:** `Code Challenge E-Commerce.csv` (Downloaded on **November 10, 2026**)  
**Total Lines in File:** 98 lines (1 Header row + 97 Data rows)

---

## 1. Executive Summary of CSV Import Results

When `Code Challenge E-Commerce.csv` is processed through the domain validation pipeline (`ecommerce.domain.csv-import` + `ecommerce.schema`):

| Metric | Count | Handling Policy |
| :--- | :---: | :--- |
| **Total Data Rows** | `97` | Parsed via RFC 4180 compliant `clojure.data.csv` |
| **Valid Rows in CSV** | `88` | Passed Malli domain coercion and validation |
| **In-Batch SKU Duplicates Merged** | `3` | Deduplicated in-memory (last write wins) before SQL `UPSERT` |
| **Unique Products Persisted** | `85` | Inserted/updated atomically via `ON CONFLICT (sku) DO UPDATE` |
| **Blank Rows Skipped** | `2` | Safely ignored without raising false-positive errors |
| **Invalid Rows Rejected** | `7` | Isolated with line-by-line error reporting; batch continues |

---

## 2. Detailed Breakdown of CSV Anomalies

### A. Rejected Rows (7 Hard Validation Failures)

Each invalid row is isolated and reported back to the client with its exact 1-based CSV line number, SKU, raw payload, and humanized Malli validation error:

| CSV Line | SKU | Product Name | Anomaly Detected | Domain Rule & Mitigation |
| :---: | :--- | :--- | :--- | :--- |
| **Line 7** | `YM-015` | `Yoga Mat` | `price` is `"free"` (non-numeric text) | Rejected by `:price` decoder (`parse-clean-double` requires valid numeric format `>= 0`). |
| **Line 16** | `DL-007` | `Desk Lamp` | `stock` is `"-5"` (negative inventory) | Rejected by Malli `[:>= 0]` constraint on `:stock` and enforced at DB level via `CHECK (stock >= 0)`. |
| **Line 20** | `XS-001` | `<script>alert('xss')</script>` | Stored XSS payload in `name` | Rejected by `no-html-tags?` validator in `NonBlankString`. React/Reagent also escapes output by default (Defense-in-Depth). |
| **Line 25** | `HD-099` | *(empty)* | Missing required `name` (`""`) | Rejected by `NonBlankString` and DB constraint `CHECK (btrim(name) <> '')`. |
| **Line 41** | `WS-001` | `"     "` | Whitespace-only `name` | Trimmed by `csv-transformer` and rejected by `NonBlankString`. |
| **Line 50** | `GK-088` | `Gaming Keyboard` | Missing `weight_kg` (`""`) | Trailing empty column padded and rejected by `:weight-kg` numeric validation. |
| **Line 52** | `GC-025` | `Gift Card` | Missing `category` (`""`) | Rejected by `NonBlankString` on `:category` and DB constraint `CHECK (btrim(category) <> '')`. |

---

### B. Coerced, Sanitized & Edge-Case Valid Rows

| CSV Line(s) | SKU | Anomaly / Edge Case | How the System Handles It |
| :---: | :--- | :--- | :--- |
| **Line 4** | `WM-042` | Currency symbol in `price` (`"$29.99"`) | `parse-clean-double` strips leading whitespace and currency prefix `$` while preserving negative signs (so negative numbers still fail `[:>= 0]`), coercing `"$29.99"` to `29.99`. |
| **Line 29** | `SQL-001` | SQL Injection payload in `name`: `Robert'); DROP TABLE products;--` | Safely persisted as a literal string because all database operations use parameterized prepared statements (`next.jdbc` / `HoneySQL`). The `products` table remains intact. |
| **Line 31** | `WB-033` | Unicode symbols (`—`, `™`) in `description` | UTF-8 reader and PostgreSQL `TEXT` column preserve international and typographic characters losslessly. |
| **Lines 2 & 36** | `RS-001` | Duplicate SKU (`Running Shoes`) with updated description, price (`89.99` -> `94.99`), and stock (`150` -> `120`) | `deduplicate-by-sku` merges duplicate SKUs within the batch (Line 36 overwrites Line 2) before executing PostgreSQL `ON CONFLICT (sku) DO UPDATE`, preventing `ERROR: ON CONFLICT DO UPDATE command cannot affect row a second time`. |
| **Lines 11, 56, 89** | `BS-021` | Triplicate SKU (`Bluetooth Speaker`) across three lines | Merged sequentially in-memory (Line 11 -> Line 56 -> Line 89) and reported in the `duplicates` audit array (`3` total in-batch merges across `RS-001` and `BS-021`). |
| **Line 47** | `MB-001` | Zero price (`0.00`) on `Mystery Box` | Allowed as a promotional/zero-cost item (`price >= 0`). |
| **Line 51** | `VC-001` | Zero stock (`0`) on `Vintage Clock` | Valid catalog entry (`stock >= 0`); UI displays **Agotado (0)** badge and disables purchase buttons until restocked via CRUD or CSV re-import. |
| **Line 53** | `CI-001` | Comma inside quoted name: `"Comma, In Product Name"` | Properly parsed by RFC 4180 compliant `clojure.data.csv` rather than naive string splitting. |
| **Line 55** | `RS-050` | Same `name` (`Running Shoes`) as `RS-001` but distinct SKU (`RS-050`) | Allowed because `sku` (not `name`) is the unique business key (`sku VARCHAR(64) NOT NULL UNIQUE`). |
| **Line 59** | `QI-001` | Escaped double quotes: `"Quote ""Inside"" Name"` | Unescaped by `clojure.data.csv` per RFC 4180 into `Quote "Inside" Name`. |
| **Lines 62 & 63** | *(none)* | Blank rows containing only commas (`,,,,,,`) | Detected via `(every? str/blank? row)` and counted under `skipped: 2`. |

---

## 3. Infrastructure & Concurrency Bugs Prevented

1. **PostgreSQL Batch Upsert Conflict (`ON CONFLICT DO UPDATE`):**
   - PostgreSQL throws an exception if a single multi-row `INSERT ... VALUES ... ON CONFLICT (sku) DO UPDATE` statement attempts to update the same target `sku` twice. Pre-aggregating by `sku` inside `ecommerce.domain.csv-import/deduplicate-by-sku` eliminates this crash while preserving audit visibility.
2. **Inventory Race Conditions on Purchase (`SELECT ... FOR UPDATE`):**
   - Concurrent checkout requests for limited-stock items (e.g. `Standing Desk` with stock `15`) are serialized at the row level using `SELECT * FROM products WHERE id = ? FOR UPDATE` inside `next.jdbc/with-transaction`.
   - Line items in a multi-product order are sorted deterministically by `product_id` prior to locking to prevent database deadlocks.
3. **Migratus Multi-Statement DDL Batch Execution:**
   - SQL migrations in `resources/migrations/001-init.up.sql` and `001-init.down.sql` use `--;;` statement separators so PostgreSQL's JDBC batch runner executes each DDL command discretely.
