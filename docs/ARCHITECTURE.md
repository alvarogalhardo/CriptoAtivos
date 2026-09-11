# Architecture

## Layers

```mermaid
flowchart TB
    Client["HTTP client<br/>(Swagger UI, curl, front end)"]

    subgraph app["Spring Boot application"]
        direction TB
        Filters["Security filter chain<br/>JWT decode · rate limit · CORS · headers"]
        Controllers["Controllers<br/>HTTP concerns only"]
        Services["Services<br/>transaction boundaries, business rules"]
        Entities["Entities<br/>own their invariants"]
        Repos["Repositories<br/>Spring Data JPA"]
        Port["PriceProvider<br/><i>port</i>"]
    end

    DB[("PostgreSQL<br/>schema owned by Flyway")]
    CG["CoinGecko"]

    Client --> Filters --> Controllers --> Services
    Services --> Entities
    Services --> Repos --> DB
    Services -.-> Port
    Port -.->|"CoinGeckoPriceProvider"| CG
    Port -.->|"ManualPriceProvider<br/>(tests, offline)"| DB
```

Controllers never touch repositories. Services never build HTTP responses. The domain never imports anything from `org.springframework.web`.

Business invariants live on the entities, not in the services. `Wallet.debit` refuses to overdraw; `AssetInventory.reserve` refuses to oversell. A service that forgot to check could not create an invalid state even if it tried, and the database `CHECK` constraints are a third line of defence.

---

## Data model

```mermaid
erDiagram
    users ||--|| wallets : "has one"
    users ||--o{ user_recovery_codes : "has many"
    users ||--o{ operations : "performed"
    wallets ||--o{ holdings : "contains"
    assets ||--|| asset_inventory : "supply"
    assets ||--o{ holdings : "held as"
    assets ||--o{ operations : "traded in"

    users {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar cpf UK
        varchar role
        boolean two_factor_enabled
        varchar two_factor_secret "AES at rest"
    }
    wallets {
        uuid id PK
        uuid user_id FK,UK
        numeric cash_balance "19,2 · CHECK >= 0"
        bigint version
    }
    holdings {
        uuid id PK
        uuid wallet_id FK
        uuid asset_id FK
        numeric quantity "19,8 · CHECK > 0"
        numeric average_cost "19,8"
    }
    assets {
        uuid id PK
        varchar asset_type "discriminator"
        varchar symbol UK
        numeric current_price "19,8"
        numeric daily_change_pct
        varchar external_id "CoinGecko id"
    }
    asset_inventory {
        uuid asset_id PK,FK
        numeric available_quantity "19,8 · CHECK >= 0"
        bigint version
    }
    operations {
        uuid id PK
        varchar operation_type "discriminator"
        uuid user_id FK
        uuid asset_id FK "TRANSACTION only"
        varchar transaction_type "BUY | SELL"
        numeric unit_price "executed price, never recomputed"
        numeric total_amount
        varchar cash_type "DEPOSIT | WITHDRAWAL"
        numeric amount
    }
```

Two single-table inheritance hierarchies, both inherited from the original design:

- `Asset → CryptoAsset` — one subclass today, but the discriminator and nullable columns mean a `StockAsset` needs no schema migration.
- `Operation → {Transaction, CashOperation}` — two real subclasses. Trades and cash movements are genuinely different events that share a user, a timestamp, and a description.

`holdings` and `asset_inventory` existed as Java classes (`CarteiraCriptoAtivos`, `EstoqueCriptoAtivos`) in the academic version but had no tables, so they constrained nothing. They are real now.

---

## Buying: the transaction

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant TC as TransactionController
    participant TS as TransactionService
    participant W as Wallet
    participant I as AssetInventory
    participant DB as PostgreSQL

    C->>TC: POST /transactions/buy {symbol, quantity}
    TC->>TS: buy(userId, symbol, quantity)

    rect rgba(120, 160, 220, 0.12)
        note over TS,DB: single @Transactional boundary
        TS->>DB: SELECT asset BY symbol
        TS->>DB: SELECT wallet FOR UPDATE
        note right of DB: lock 1 — wallet
        DB-->>W: locked row

        TS->>W: debit(quantity × currentPrice)
        alt insufficient funds
            W--xTS: BusinessRuleException
            note over TS,DB: rollback — nothing recorded
        end

        TS->>DB: SELECT inventory FOR UPDATE
        note right of DB: lock 2 — inventory<br/>(always after the wallet)
        TS->>I: reserve(quantity)
        alt insufficient supply
            I--xTS: InsufficientInventoryException
            note over TS,DB: rollback — the debit is undone too
        end

        TS->>W: addHolding(asset, quantity, unitPrice)
        TS->>DB: INSERT transaction (executed price stored)
    end

    TS-->>TC: Transaction
    TC-->>C: 201 Created
```

The price is snapshotted once at execution and written to the transaction row. History therefore never changes retroactively when the market moves — the original design read the price back off the asset, so past trades appeared to change price.

---

## Concurrency

Two row locks are involved in a trade, and **the order is always wallet, then inventory**. `TransactionService` is the only class that holds both. If any future code path reversed the order, two concurrent trades on different assets could deadlock — intermittently, and painfully hard to reproduce.

Pessimistic locking rather than optimistic is deliberate. Optimistic locking answers a conflict with "retry your request", which is the wrong answer for money: the caller has no idea whether their purchase happened. Serialising for a few milliseconds is better than a retry loop over a financial operation.

Three independent defences stop a balance going negative:

1. `Wallet.debit` throws before mutating.
2. The `FOR UPDATE` lock serialises concurrent access.
3. `CHECK (cash_balance >= 0)` in Postgres rejects it even if the first two were bypassed.

`TransactionIntegrityIT` proves the first two under real contention: eight threads released simultaneously by a `CyclicBarrier` against a balance that affords exactly one purchase, asserting exactly one succeeds.

---

## Authentication

```mermaid
stateDiagram-v2
    [*] --> Anonymous

    Anonymous --> Anonymous: POST /auth/login<br/>wrong credentials → 401

    Anonymous --> Authenticated: POST /auth/login<br/>2FA disabled → access token

    Anonymous --> Challenged: POST /auth/login<br/>2FA enabled → challenge token

    Challenged --> Authenticated: POST /auth/2fa/verify<br/>valid TOTP or recovery code
    Challenged --> Challenged: invalid code → 401
    Challenged --> Anonymous: challenge expires (5 min)

    Authenticated --> [*]: token expires (2 h)

    note right of Challenged
        Carries ROLE_2FA_CHALLENGE only.
        Rejected by every protected route —
        pinned by a test.
    end note
```

Access tokens are RS256-signed, so the public key verifies without being able to mint. There is no `UserDetailsService` and no session: authentication is signature verification, not a database lookup per request.

TOTP secrets are AES-encrypted at rest and recovery codes are BCrypt-hashed, so a database dump alone cannot produce a valid second factor.

---

## Testing strategy

| | `*Test` | `*IT` |
|---|---|---|
| Spring context | no | yes |
| Database | none | real PostgreSQL (Testcontainers) |
| Runs in | `surefire` | `failsafe` |
| Speed | milliseconds | seconds |

Integration tests share one container and one cached Spring context, so the suite pays that cost once.

Most `*IT` classes are `@Transactional` and roll back. Three deliberately are **not**:

- **`TransactionIntegrityIT`** — rollback assertions are meaningless inside a test transaction, because the work is only flushed and never committed. Concurrency needs real commits visible across threads.
- **`WalletReadIT`** — a test transaction keeps the Hibernate session open, hiding `LazyInitializationException` that real callers hit with `open-in-view` disabled. This is how the lazy-loading bug in `GET /api/v1/wallet` was found.
- **`AdminUserIT`** — deletion cascades across four tables and the guardrails depend on committed state.

These clean up explicitly in `@AfterEach`. The extra bookkeeping buys tests that fail for the same reasons production would.
