# Ledger

Spring Boot + PostgreSQL + Flyway + Thymeleaf: accounts, deposits, transfers, reversals, audit log. Single server-rendered UI.

## Run locally

1. **Start Postgres** (Docker):

   ```bash
   docker compose up -d
   ```

2. **Run the app** (Java 17+). From the project directory use the included wrapper (no global Maven required):

   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   ./mvnw spring-boot:run
   ```

   The file `.mvn/maven.config` makes the wrapper use **Maven Central** via `.mvn/settings-central.xml`. If you prefer your own `settings.xml`, run `mvn spring-boot:run` instead.

   If your shell still points `JAVA_HOME` at an old JDK, set it to Temurin 17 (see `/usr/libexec/java_home -V`).

3. Open **http://localhost:8080**

There is **no seeded data** — create accounts first; you need two accounts before transfers are available.

If Flyway fails after a schema change (e.g. missing migration file), reset the volume: `docker compose down -v && docker compose up -d`.

- **V3** removes legacy demo account names if they still exist.
- **V4** resyncs the `accounts` id sequence after deletes (Postgres sequences do not rewind by default).

Defaults match `docker-compose.yml` (`ledger` / `ledger`; Postgres on host **5433**). Override with `SPRING_DATASOURCE_*` if needed.

## Assumptions

- **Money** is stored as `NUMERIC(19,2)` and mapped with `BigDecimal` (no floating point for currency).
- **Transfer** rows exist only for **successful** movements; failed attempts appear only in `audit_log`.
- **Reversal** inverts the movement (credit original sender, debit original recipient). A reversal fails if the recipient no longer has enough balance (edge case if money moved again after the original transfer). Re-running undo on the same transfer is **idempotent** (no double correction; audit records a no-op with SUCCESS).
- **Account create** — audit `ACCOUNT_CREATE`; optional non-negative opening balance credits the new account in the same transaction (money “appears” on the ledger once).
- **Deposit** — audit `DEPOSIT`; `from_account_id` is null (external / not another ledger account). Locks the target row for the balance update.
- **Auth** is omitted; this is a local demo.

---

## Submission checklist (original brief)

- Real DB with migrations — yes (Postgres + Flyway).
- Browser UI wired to backend — yes (Thymeleaf).
- Audit log for balance-changing attempts (incl. failures where recorded) — yes; see `audit_log` and operation types.
- Concurrent-safe transfers + idempotent reversal — yes (see README table).
- README with run, schema/migrations, concurrency notes, design trade-offs — this file.

Extra flows (**create account**, **deposit**) are optional polish and match “money in/out of the system”; document them in review as below.

## Design choices (what to stand behind in review)

| Topic | Choice here | Why |
|--------|----------------|-----|
| **Correctness under concurrency** | One DB transaction per operation; **`PESSIMISTIC_WRITE`** (`SELECT … FOR UPDATE`) on both account rows | Serialize conflicting work on the same rows. Simpler to explain than optimistic retry loops for this scope. |
| **Deadlock avoidance** | Always lock the two accounts in **sorted order by id** | Classic pattern when two transactions touch the same pair of rows in opposite order. |
| **Atomicity** | Debit/credit + `transfers` insert + success **`audit_log`** in the **same** `@Transactional` method | All-or-nothing with Postgres; balances and audit stay aligned. |
| **Failures & audit** | Validation failures still **`INSERT` into `audit_log`** with `FAILURE` where applicable | Matches the assignment: auditable failed attempts, not only successes. |
| **Undo idempotency** | `transfers.reversed` flag; lock transfer row before mutating | Second undo sees `reversed = true`, records audit, returns without moving money again. |
| **Stack** | Spring Boot + JPA + Thymeleaf | Minimal moving parts for Java developers; one process for API + UI demo. |
| **Schema** | Flyway SQL migrations | Reviewers can read exact DDL and constraints; no “magic” schema from Hibernate alone. |
| **Create / deposit** | `ACCOUNT_CREATE` and `DEPOSIT` audit types; deposit locks one account | Fits “funds in/out of the system” without inventing a second counterparty row. |

What you might add later (only if asked): **optimistic locking** (`@Version`) as a belt-and-suspenders check, **outbox** for async projections, or **integration tests** hitting Postgres (e.g. Testcontainers) plus a small concurrent stress test in CI.

## Try concurrency manually

With the app running, many parallel transfers from the same account (e.g. with insufficient total balance) should yield consistent balances and a mix of SUCCESS/FAILURE audit rows—never a negative balance.

Example with `curl` (replace account `1` → `2`, tune amount and count):

```bash
seq 1 20 | xargs -P 10 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST http://localhost:8080/transfer \
  -d "fromId=1&toId=2&amount=50" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

Then refresh the home page: inspect **Accounts**, **Recent transfers**, and **Audit log**.

## Layout

- `src/main/java/com/level/ledger` — application, web, service, JPA entities, repositories
- `src/main/resources/db/migration` — Flyway migrations
- `src/main/resources/templates/index.html` — UI
