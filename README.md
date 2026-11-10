# Enterprise E-Commerce Platform

**CSV Dataset Reference:** The example file `Code Challenge E-Commerce.csv` was downloaded on **November 10, 2026**.

## Local Setup Instructions

1. Ensure Docker and Docker Compose are installed.
2. Run the application:
   ```bash
   docker compose up --build
   ```
3. Access the web interface at http://localhost:8080.

## Architecture Overview

This project implements a Hexagonal (Ports and Adapters) Architecture using Clojure and ClojureScript.

 - Domain: Data validation using Malli, shared between frontend and backend.

 - Adapters: PostgreSQL with next.jdbc, Ring/Reitit for HTTP, and a purely functional CSV parser.

 - State Management: Integrant for lifecycle management.