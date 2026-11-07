# Locking Demo: flight seat booking

Spring Boot 3.3 (Java 21) + PostgreSQL backend, plus a vanilla HTML/CSS/JS dashboard.
Shows optimistic locking, pessimistic locking, transactions and isolation levels.
The dashboard is dark-themed and works on phones. Swagger UI is included for exploring the API.

## Project structure
```
locking-demo/
├── backend/                     Spring Boot app (pom.xml, application.yml)
│   └── src/main/java/com/example/locking/
│       ├── entity/              Seat (@Version)
│       ├── repository/          SeatRepository (PESSIMISTIC_WRITE query)
│       ├── service/             SeatService, ConcurrentDemoService
│       ├── controller/          SeatController, DemoController
│       ├── dto/                 Dtos (request/response records)
│       ├── exception/           Custom exceptions + @RestControllerAdvice
│       └── config/              CORS, sample data seeding
│   └── src/test/                Testcontainers integration test
├── frontend/                    index.html, styles.css, script.js (no framework)
├── .gitignore
└── README.md
```

## Run it
Prerequisites: JDK 21, Maven 3.9+, Docker.

```bash
# 1. Database
docker run -d --name lockingdb -p 5432:5432 -e POSTGRES_DB=lockingdb -e POSTGRES_PASSWORD=postgres postgres:16-alpine

# 2. Backend (seeds flight FL100, 36 seats, on first start)
cd backend && mvn spring-boot:run

# 3. Frontend (any static server, or just open frontend/index.html)
cd frontend && python3 -m http.server 3000      # then open http://localhost:3000

# Tests (Testcontainers starts its own PostgreSQL; Docker must be running)
cd backend && mvn verify
```

## Useful URLs (backend running)
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Dashboard: http://localhost:3000 (or open `frontend/index.html` directly)

## Simulation vs real
- **Simulation** (dashboard sections: optimistic, pessimistic, isolation): JavaScript animation only. No backend call.
- **Real API** (seat map and "Run it for real"): fetch() to Spring Boot, which hits PostgreSQL. Results are never faked in JS.

## API
| Method | Path | Notes |
|---|---|---|
| POST | /api/seats | `{"seatNumber":"7A","flightNumber":"FL100"}` |
| GET | /api/seats, /api/seats/{id} | |
| POST | /api/seats/{id}/book/optimistic | `{"userName":"ann","expectedVersion":0}` → 409 on conflict |
| POST | /api/seats/{id}/book/pessimistic | `{"userName":"ann"}` → 423 on lock timeout |
| POST | /api/seats/{id}/reset | |
| POST | /api/demo/concurrent-booking/{optimistic\|pessimistic} | `{"seatId":1,"users":20}` |

```bash
curl -X POST localhost:8080/api/seats/1/book/optimistic -H 'Content-Type: application/json' -d '{"userName":"ann","expectedVersion":0}'
curl -X POST localhost:8080/api/demo/concurrent-booking/pessimistic -H 'Content-Type: application/json' -d '{"seatId":1,"users":20}'
```
HTTP mapping: 409 version conflict or seat already booked, 423 lock timeout, 404 not found, 400 bad request.
Note: Hibernate starts `@Version` at 0 (the visual simulation says "version 1" for readability).

## How it works (interview notes)
**Optimistic.** No DB lock. Every UPDATE becomes `update seats set ..., version=? where id=? and version=?`. Zero rows updated means someone committed first, so Hibernate throws `StaleObjectStateException`, which Spring translates to `ObjectOptimisticLockingFailureException`. Best when conflicts are rare; handle by retrying (re-read, re-apply, bounded attempts).

**Pessimistic.** `@Lock(PESSIMISTIC_WRITE)` makes Hibernate issue `select ... for update`. PostgreSQL takes a row lock; other writers (and other FOR UPDATE readers) block until the holder commits or rolls back. The lock lives exactly as long as the transaction, so transactions must be short. PostgreSQL ignores Hibernate's lock-timeout hint, so `application.yml` sets `lock_timeout` per connection; a timeout surfaces as `PessimisticLockingFailureException`.

**Transactions.** `@Transactional` is proxy-based: commit on return, rollback on unchecked exceptions, and no effect on self-invocation. Locks and the version check only work inside the transaction, which is why each service method is its own transaction.

**Multiple instances.** `synchronized` and `ReentrantLock` are per JVM, so two instances never see each other's lock. Row locks and version checks live in the database, so they work across any number of instances.

**Why Testcontainers, not Mockito.** Row locks, MVCC and "rows updated = 0" exist only inside a real database engine. A mock returns whatever you tell it.

**Pitch for an interview:** "I protected a shared row two ways, compared their failure modes under 20 concurrent requests against real PostgreSQL, and proved exactly one booking wins in both cases."

## Dashboard tour
1. **Choose a seat** (real): pick a seat, book it with either locking strategy.
2. **Optimistic / Pessimistic** (simulation): step through or auto-run the animated timelines.
3. **Run it for real** (real): 10 or 20 concurrent users; the table shows who won and why the others failed.
4. **Isolation** and **Java vs DB** (explanation): pick an isolation level; see why `synchronized` fails across instances.

## Troubleshooting
- Frontend shows "Network error": backend not running on 8080, or Postgres is down.
- `password authentication failed`: match credentials in `application.yml`.
- Pessimistic run returns TIMEOUT: raise `lock_timeout` or lower `users`.
- Swagger page 404: confirm the `springdoc-openapi-starter-webmvc-ui` dependency is in `backend/pom.xml` and rebuild.
- Port 5432 already in use: stop your local PostgreSQL or map Docker to another port and update `application.yml`.
- Optimistic run shows few conflicts: raise `demo.work-millis`.
