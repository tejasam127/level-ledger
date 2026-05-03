# Ledger

Take-home: **Spring Boot 3**, **PostgreSQL**, **Flyway**, **Thymeleaf**. Accounts, deposits, transfers, idempotent reversals, and an **audit log**. UI is server-rendered (no SPA).

## Run

**Requires:** Java 17+, Docker.

```bash
docker compose up -d
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw spring-boot:run
```

Open **http://localhost:8080**. Create accounts first (no seed data); you need two accounts to transfer.

- Postgres is on host **5433** (see `docker-compose.yml`). JDBC defaults match: DB/user/password `ledger`.
- Wrapper uses **Maven Central** via `.mvn/settings-central.xml` (see `.mvn/maven.config`). Override DB with `SPRING_DATASOURCE_URL` etc. if needed.
- Flyway trouble after a schema change: `docker compose down -v && docker compose up -d`

## Design (short)

- **Concurrency:** one transaction per operation; **`SELECT … FOR UPDATE`** on involved accounts; always lock the **two accounts in ascending id order** to avoid deadlocks.
- **Transfers:** debit/credit + `transfers` row + **audit** in the same transaction. Failed validations are **audited** where implemented.
- **Reversal:** row locked; **`reversed`** flag; repeating undo is a **no-op** (audit still records it).
- **Money:** `NUMERIC` / `BigDecimal`. **Auth** not included.

**Concurrency smoke test** (replace account ids; app must be running):

```bash
seq 1 20 | xargs -P 10 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8080/transfer \
  -d "fromId=1&toId=2&amount=50" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

## Layout

| Path | Purpose |
|------|--------|
| `src/main/java/com/level/ledger` | App, controllers, `LedgerService`, JPA entities & repos |
| `src/main/resources/db/migration` | Flyway SQL |
| `src/main/resources/templates/` | Thymeleaf HTML |
