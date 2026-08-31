# FX Service

A Spring Boot REST API for foreign-exchange rates and client currency conversions.

The service retrieves exchange rates from an external FX provider, performs conversions against client balances, persists conversion history, and provides idempotent and concurrency-safe processing.

The implementation focuses on the assignment requirements while keeping the architecture intentionally simple.

---

## Features

* Exchange rates from Frankfurter
* Currency conversion against client balances
* PostgreSQL persistence with Flyway migrations
* Redis caching of exchange rates
* Pessimistic row-level locking
* Database-backed idempotency
* Paginated conversion history
* ISO-4217 currency validation
* Consistent API error responses
* Provider timeouts and limited retries
* OpenAPI / Swagger UI
* Spring Boot Actuator
* Docker Compose
* Multi-stage, non-root Docker image
* Unit and integration tests
* Testcontainers PostgreSQL integration tests

---

## Technology Stack

* Java 21
* Spring Boot 3.x
* Spring Web / REST
* Spring Data JPA / Hibernate
* PostgreSQL
* Flyway
* Redis / Spring Cache
* Maven
* JUnit 5 / Mockito
* MockMvc
* Testcontainers
* OpenAPI / Swagger
* Spring Boot Actuator
* Docker

---

# API

## Get Exchange Rate

```http
GET /rates?from=USD&to=EUR
```

Example:

```json
{
  "from": "USD",
  "to": "EUR",
  "rate": 0.8500
}
```

Rates are retrieved from the external provider and cached in Redis for five minutes.

---

## Create Conversion

```http
POST /conversions
```

Required headers:

```http
X-Client-Id: CLIENT-001
Idempotency-Key: example-key-001
Content-Type: application/json
```

Request:

```json
{
  "sourceCurrency": "USD",
  "targetCurrency": "EUR",
  "amount": 100.00
}
```

Example response:

```json
{
  "transactionId": "d66d1491-3627-4e5b-9bf2-e4e6b13f304c",
  "sourceAmount": 100.0000,
  "sourceCurrency": "USD",
  "targetAmount": 85.0000,
  "targetCurrency": "EUR",
  "rate": 0.8500,
  "timestamp": "2026-08-31T13:07:24.181075Z",
  "balances": [
    {
      "currency": "USD",
      "amount": 9900.0000
    },
    {
      "currency": "EUR",
      "amount": 8085.0000
    }
  ]
}
```

The balance updates, conversion record, and idempotency record are committed within the same database transaction.

---

## Conversion History

```http
GET /conversions?transactionId=...&date=2026-08-31&clientId=CLIENT-001&page=0&size=20
```

At least one filter must be provided.

Supported filters:

* `transactionId`
* `date`
* `clientId`

Results are paginated using Spring `Pageable`.

---

## Client Balances

```http
GET /clients/{clientId}/balances
```

Example:

```http
GET /clients/CLIENT-001/balances
```

Returns the current balance for each currency held by the client.

---

# Demo Clients

Flyway migrations seed demo clients and balances.

| Client     | Currency |     Balance |
| ---------- | -------- | ----------: |
| CLIENT-001 | USD      | 10,000.0000 |
| CLIENT-001 | EUR      |  8,000.0000 |
| CLIENT-002 | GBP      |  5,000.0000 |

These accounts can be used immediately after starting the application.

---

# Concurrency and Idempotency

## Pessimistic Locking

Conversions use PostgreSQL row-level locking with `SELECT ... FOR UPDATE`.

The exchange rate is retrieved **before acquiring the database locks**, so an external HTTP call does not unnecessarily extend the lock duration.

Once the locks are acquired, the service:

1. Checks idempotency again.
2. Validates the balance.
3. Calculates the target amount.
4. Debits the source balance.
5. Credits the target balance.
6. Persists the conversion.
7. Persists the idempotency record.
8. Commits the transaction.

Concurrent conversions involving the same balance rows are therefore serialized.

For example, with:

```text
USD = 100
```

two concurrent requests attempting to convert:

```text
80 USD
```

cannot both succeed. One transaction completes first; the second waits for the lock and then observes the updated balance, resulting in `INSUFFICIENT_FUNDS`.

---

## Idempotency

`POST /conversions` supports the `Idempotency-Key` header.

The combination of:

```text
clientId + idempotencyKey
```

is unique in PostgreSQL.

An idempotency record stores the transaction ID of the original conversion.

The service performs an initial idempotency check before retrieving the rate and a second check after acquiring the balance locks.

The second check handles concurrent requests where another request completes the conversion while the first request is retrieving the exchange rate.

The database unique constraint provides an additional integrity guarantee against duplicate idempotency records.

Because idempotency is database-backed, the mechanism works across multiple application instances.

---

# Rate Provider and Caching

The application uses Frankfurter as the external FX provider.

The provider is hidden behind an `ExchangeRateProvider` interface so the business logic is not coupled directly to the external API.

The HTTP client uses explicit connection and response timeouts and limited retries for transient provider failures.

If the provider remains unavailable after the configured retries, the failure is translated into `RATE_PROVIDER_ERROR`.

Exchange rates are cached using Redis and Spring Cache:

```properties
spring.cache.type=redis
spring.cache.redis.time-to-live=5m
```

The cache key is based on the currency pair, for example:

```text
USD-EUR
```

Redis is treated only as a cache. PostgreSQL remains the source of truth for clients, balances, conversions, and idempotency records.

---

# Money and Validation

Monetary values use `BigDecimal`; `double` and `float` are not used for financial calculations.

* Monetary amounts use `NUMERIC(19, 4)`
* Exchange rates use `NUMERIC(19, 10)`
* Amounts are limited to four decimal places
* Target amounts use `RoundingMode.HALF_UP`

Currencies are normalized to uppercase and validated against supported ISO-4217 codes.

Invalid or non-positive amounts, unknown currencies, blank currencies, and same-currency conversions are rejected.

---

# Error Handling

The API uses a consistent error response structure and does not expose raw stack traces.

Application error codes include:

* `CLIENT_NOT_FOUND`
* `BALANCE_NOT_FOUND`
* `INSUFFICIENT_FUNDS`
* `INVALID_CURRENCY`
* `INVALID_AMOUNT`
* `RATE_PROVIDER_ERROR`

Examples:

```text
CLIENT_NOT_FOUND       → 404 Not Found
BALANCE_NOT_FOUND      → 404 Not Found
INSUFFICIENT_FUNDS     → 422 Unprocessable Entity
```

Provider failures are translated into an application-specific error after the configured retry attempts are exhausted.

---

# Database

PostgreSQL is the primary source of truth.

Flyway manages the database schema and demo data. Hibernate is not used to modify the schema automatically.

Main entities:

```text
Client
Balance
Conversion
IdempotencyKey
```

A conversion stores the exchange rate and resulting balances so that historical conversion data remains independent of future balance changes.

---

# Running Locally

## Prerequisites

* Java 21
* Docker / Docker Compose
* Maven, or use the included Maven Wrapper

## Start

```bash
docker compose up --build
```

This starts PostgreSQL, Redis, and the FX Service.

The API is available at:

```text
http://localhost:8080
```

## Stop

```bash
docker compose down
```

To remove the PostgreSQL volume and start with a clean database:

```bash
docker compose down -v
```

---

# Swagger and Actuator

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Health:

```text
http://localhost:8080/actuator/health
```

Metrics:

```text
http://localhost:8080/actuator/metrics
```

---

# Testing

The project contains unit and integration tests.

Unit tests cover:

* conversion logic
* validation
* currency validation
* rate service
* exchange-rate provider
* transaction logic
* controller validation/error handling
* retry behavior

Integration tests use Testcontainers with real PostgreSQL and cover:

* Flyway migrations
* successful conversions
* insufficient funds
* unknown clients
* idempotency
* concurrent conversions
* concurrent idempotent requests
* rate endpoint
* Redis caching
* balances
* conversion history
* filtering and pagination

Run all tests:

```bash
./mvnw test
```

Windows:

```powershell
.\mvnw.cmd test
```

Docker must be available for Testcontainers.

Current line coverage is approximately **96%**.

---

# Docker

The application uses a multi-stage Docker build.

The build stage compiles the application using a JDK. The runtime stage contains only the Java runtime and application JAR.

The application runs as a dedicated non-root user:

```text
appuser
```

Build the image:

```bash
docker build -t fx-service .
```

Or:

```bash
docker compose build
```

---

# Design Decisions

### PostgreSQL

PostgreSQL was chosen instead of H2 because the application relies on real transactional and row-locking semantics. Testcontainers uses PostgreSQL for integration tests as well.

### Pessimistic Locking

Pessimistic locking makes concurrent balance updates explicit and prevents double-spending. The trade-off is that operations involving the same balance rows are serialized.

### Idempotency

Idempotency is enforced at both the application and database levels. The second check after acquiring the balance locks protects against concurrent duplicate requests.

### Redis

Redis is used only as a rate cache. PostgreSQL remains the source of truth, so cache loss does not affect business state.

### External Provider

The provider is abstracted behind an interface. Timeouts and limited retries prevent indefinite waits and provide basic resilience against transient failures.

---

# What I Would Do With More Time

For a production system, I would consider:

* Circuit breaking and more advanced provider resilience
* Structured logging, correlation IDs, and additional metrics
* Authentication, authorization, and rate limiting
* CI/CD with static analysis and container scanning
* Production-based database, Redis, and connection-pool tuning
* Stricter exchange-rate freshness/versioning

These are outside the scope of the assignment.

---

# Project Structure

```text
src/main/java
└── com.phh.fx_service
    ├── client
    ├── balance
    ├── conversion
    ├── rate
    ├── validation
    ├── exception
    └── config

src/main/resources
└── db/migration

src/test/java
└── com.phh.fx_service
    ├── unit
    └── integration

Dockerfile
docker-compose.yml
pom.xml
README.md
```
