# CriptoAtivos → Production-Grade REST API: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Context

CriptoAtivos is an academic Java OOP exercise: a single-user console app that registers a user, holds a cash balance, and buys/sells crypto assets typed in by hand. The owner wants to put it on their resume, so it needs to stop looking like coursework and start looking like a service an engineer would be paid to maintain.

### Two divergent codebases exist

| | `C:\Users\MICRO\Desktop\CriptoAtivos` (local) | `github.com/alvarogalhardo/CriptoAtivos` (remote) |
|---|---|---|
| Version control | **none** — no `.git`; `.idea/vcs.xml` maps a Git root that doesn't exist | 2 commits, `main`, Apr 26 2025 |
| Build | none — IntelliJ `.iml` + committed `.class` files in `out/` | Maven `pom.xml`, `org.example`, Java 24, `ojdbc8` |
| Layout | flat default package, 9 classes | `org/example/{config,dao,enums,models}` + `Main.java` |
| Persistence | in-memory only | Oracle JDBC, 4 DAOs |
| Models | 8 | 10 — adds `CarteiraCriptoAtivos`, `EstoqueCriptoAtivos`, `DetalhesTransacao`, `DetalhesCriptoAtivos` |
| Menu | 7 options | 9 options, incl. 2FA login and a DAO self-test routine |

**The remote is the more advanced iteration; the local folder is an earlier draft.** This plan therefore builds on the GitHub repository, preserving its history. The local folder is left untouched and optionally archived onto a branch (Task 1, Step 6) so no work is lost.

**What's wrong in the code today** (verified by reading every local file; the remote shares the same domain flaws on top of a DAO layer):

File references below point at the **local draft**, where the code was read line by line. The remote reorganises the same classes into `org/example/models` and adds DAOs; every domain flaw listed survives that reorganisation because none of it was fixed, only moved.

| Problem | Where |
|---|---|
| `org.example` groupId and Java 24 (a non-LTS release) — a placeholder build | remote `pom.xml` |
| Oracle JDBC (`ojdbc8`) for a portfolio project — a licensed database nobody reviewing this will run | remote `pom.xml` |
| `.idea/` committed to the repository | remote repo root |
| Money and quantities as `double` — rounding drift in a *financial* app | `Carteira.java:12,33-64`, `Ativos.java:4` |
| `Autenticacao.java` is dead code: 4 fields, zero methods, never instantiated | `Autenticacao.java:1-6` |
| `Usuario.autenticar()` is an empty method body; passwords stored in plaintext | `Usuario.java:41-43,60-62` |
| Purchase recorded even when it fails — `ativos.add()` runs before the balance check, and "Compra realizada com sucesso" always prints | `Main.java:101-103` vs `Carteira.java:39-41` |
| Sells at the price captured when the asset was created, never a current price | `Carteira.java:64` |
| `HashMap<CriptoAtivo, Double>` with no `equals`/`hashCode` — buying BTC twice creates two unrelated holdings | `Carteira.java:9`, `CriptoAtivo.java` |
| `Transacao` objects built then discarded; `executarOperacao()` never called; `transacaos` list never written to | `Main.java:100`, `Carteira.java:43` |
| `this.data = data` assigns the field to itself | `Operacao.java:14` |
| Non-numeric input at the menu throws `InputMismatchException` outside the `try`, and the bad token stays in the buffer → infinite loop | `Main.java:24` |
| `leitor.next()` for names — "Ana Maria" silently becomes "Ana" | `Main.java:58` |
| No tests, no persistence, no validation, no logging | everywhere |

**Outcome:** a Spring Boot 3 REST API with English domain naming, PostgreSQL + Flyway, JWT auth over Spring Security with TOTP two-factor, `BigDecimal` money, exchange-side asset inventory, admin user management, live CoinGecko prices, Testcontainers-backed integration tests, Docker Compose, CI, and a README that explains the design decisions.

**Goal:** Rebuild CriptoAtivos as a documented, tested, containerized crypto-portfolio REST API that a hiring engineer can clone, run with one command, and read without wincing.

**Architecture:** Package-by-feature Spring Boot service (`auth`, `user`, `asset`, `wallet`, `operation`, `admin`, `common`). Controllers hold only HTTP concerns; services own transactions and business rules; JPA repositories own persistence. The original OOP hierarchies are *kept and justified*, not discarded: `Asset` → `CryptoAsset` and `Operation` → {`Transaction`, `CashOperation`} become JPA single-table inheritance, which turns the coursework abstractions into real extension points. Three concepts are carried forward from the remote version: 2FA login becomes a real TOTP challenge flow, `EstoqueCriptoAtivos` becomes an `AssetInventory` that buys draw down, and the list/delete-user menu entries become admin-only endpoints. External price data enters through a `PriceProvider` port so the domain never talks HTTP.

**Tech Stack:** Java 21 · Spring Boot 3.5.x (Web, Data JPA, Security + OAuth2 Resource Server, Validation, Actuator, Cache) · PostgreSQL 16 · Flyway · `java-otp` (TOTP) · springdoc-openapi · JUnit 5 · AssertJ · Testcontainers · Maven (wrapper) · Docker Compose · GitHub Actions · JaCoCo · Spotless

**Spec:** This document is self-contained — the Context section above is the spec.

**Repository:** `https://github.com/alvarogalhardo/CriptoAtivos` — public, `main`, currently at `2dbb72b`.

---

## Global Constraints

Every task's requirements implicitly include this section.

- **Working directory:** `C:\Users\MICRO\Desktop\CriptoAtivos-api` — a fresh clone of the GitHub repo (Task 1). **Not** the existing `Desktop\CriptoAtivos` folder, which stays untouched as the old draft.
- **Branch:** all work lands on `feat/production-revamp`, merged to `main` via a pull request at the end (Task 16). Never commit directly to `main` after the baseline tag.
- **Base package:** `com.criptoativos` (replacing the repo's `org.example`). groupId `com.criptoativos`, artifactId `criptoativos-api`. If a published-artifact-correct name is preferred, `io.github.alvarogalhardo.criptoativos` is the alternative — pick one at Task 1 and never mix.
- **Java language level:** 21 (`<java.version>21</java.version>`). Installed JDK is Corretto 22 — compiling with `--release 21` on it is correct and expected. Docker images use `eclipse-temurin:21`.
- **Build tool:** Maven **wrapper only** (`./mvnw`, `mvnw.cmd`). Neither `mvn` nor `gradle` is on this machine's PATH; never write a command that assumes they are.
- **Shell:** Windows. Give PowerShell-safe commands; `&&` does **not** chain in Windows PowerShell 5.1 — use `;` or separate lines. Git Bash is available for `openssl`.
- **Money:** never `double`/`float` anywhere. Cash = `BigDecimal` scale 2, `NUMERIC(19,2)`. Quantities and unit prices = `BigDecimal` scale 8, `NUMERIC(19,8)`. Rounding is always `RoundingMode.HALF_EVEN`, applied through `com.criptoativos.common.Money`.
- **Identifiers:** all entity IDs are `UUID`, generated by Postgres `gen_random_uuid()`.
- **Timestamps:** `Instant` in Java, `timestamptz` in Postgres. Never `java.util.Date`.
- **Naming:** English throughout — `Wallet`, `CryptoAsset`, `Transaction`, `Operation`, `User`, `BUY`/`SELL`. The Portuguese product name "CriptoAtivos" stays as the project/brand name only.
- **DTOs:** Java `record`s with Bean Validation annotations. Map with static factory methods (`static WalletResponse from(Wallet w)`). No Lombok, no MapStruct — no annotation processors, so the project opens cleanly in any IDE.
- **Entities:** plain classes, `protected` no-arg constructor for JPA, static factory for real construction, no public setters on invariant-bearing fields.
- **API base path:** `/api/v1`.
- **Errors:** every error response is an RFC 7807 `ProblemDetail`. No stack traces or raw exception messages cross the wire.
- **Test naming:** `*Test.java` = unit (no Spring context). `*IT.java` = integration (Spring context + Testcontainers Postgres), run by `failsafe` during `./mvnw verify`.
- **Commits:** Conventional Commits (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `refactor:`). Commit at the end of every task, minimum.

---

## File Structure

```
CriptoAtivos/
├── .github/workflows/ci.yml
├── .gitignore                          # rewritten: Maven + IntelliJ + secrets
├── .mvn/wrapper/                       # Maven wrapper
├── mvnw, mvnw.cmd
├── pom.xml
├── compose.yaml                        # postgres + api
├── Dockerfile                          # multi-stage
├── README.md
├── docs/
│   ├── ARCHITECTURE.md
│   ├── plans/2026-09-10-production-revamp.md   # copy of this file
│   └── api.http                        # runnable request collection
└── src/
    ├── main/java/com/criptoativos/
    │   ├── CriptoAtivosApplication.java
    │   ├── common/
    │   │   ├── Money.java                      # scale/rounding helpers
    │   │   ├── ApiExceptionHandler.java        # @RestControllerAdvice → ProblemDetail
    │   │   ├── exception/                      # DomainException + subclasses
    │   │   └── validation/{Cpf,CpfValidator}.java
    │   ├── config/{SecurityConfig,JwtConfig,OpenApiConfig,CacheConfig,SchedulingConfig,WebConfig}.java
    │   ├── auth/
    │   │   ├── {AuthController,AuthService,TokenService}.java + dto/
    │   │   └── twofactor/{TotpService,TwoFactorService,TwoFactorController,RecoveryCode,
    │   │                  RecoveryCodeRepository}.java + dto/
    │   ├── user/{User,Role,UserRepository,UserService,UserController}.java + dto/
    │   ├── admin/{AdminUserController,AdminUserService,AdminSeeder}.java + dto/
    │   ├── asset/
    │   │   ├── {Asset,CryptoAsset,AssetRepository,AssetService,AssetController}.java + dto/
    │   │   ├── inventory/{AssetInventory,AssetInventoryRepository,AssetInventoryService,
    │   │   │             AssetInventoryController}.java + dto/
    │   │   └── price/{PriceProvider,CoinGeckoPriceProvider,ManualPriceProvider,PriceRefreshJob}.java + dto/
    │   ├── wallet/{Wallet,Holding,WalletRepository,WalletService,WalletController,PortfolioService}.java + dto/
    │   └── operation/{Operation,Transaction,CashOperation,TransactionType,CashOperationType,
    │                  OperationRepository,TransactionService,TransactionController}.java + dto/
    ├── main/resources/
    │   ├── application.yml, application-dev.yml, application-prod.yml
    │   └── db/migration/V1__baseline.sql, V2__seed_assets.sql
    └── test/
        ├── java/com/criptoativos/           # mirrors main; *Test + *IT
        │   └── support/{AbstractIT,TestFixtures}.java
        └── resources/application-test.yml
```

**Deleted from the clone:** `src/main/java/org/example/**` (Main + `config`/`dao`/`enums`/`models`), `.idea/`. Their content is preserved forever at the `v0-academic-baseline` tag and referenced from the README. The Oracle `ojdbc8` dependency goes with them — Postgres replaces it.

**Task-to-file map:**

| Task | Primary deliverable |
|---|---|
| 1 | Clone, branch, Maven scaffold, `com.criptoativos` package |
| 2 | `compose.yaml`, `V1__baseline.sql`, `AbstractIT` |
| 3 | `common/` — `Money`, `ApiExceptionHandler`, exceptions, `@Cpf` |
| 4 | `user/` — registration |
| 5 | `config/SecurityConfig`, `auth/` — JWT login |
| 6 | `auth/twofactor/` — TOTP enrolment, challenge, recovery codes |
| 7 | `asset/` — catalogue |
| 8 | `asset/inventory/` — exchange-side supply |
| 9 | `wallet/`, `operation/CashOperation` — deposits and withdrawals |
| 10 | `operation/` — buy and sell |
| 11 | `asset/price/` — CoinGecko |
| 12 | `wallet/PortfolioService` — P&L |
| 13 | `admin/` — user management, `AdminSeeder` |
| 14 | `config/OpenApiConfig`, `docs/api.http` |
| 15 | `Dockerfile`, CI, quality gates |
| 16 | `README.md`, `docs/ARCHITECTURE.md`, PR and merge |

---

## Task 1: Clone, tag the baseline, and scaffold Maven

Task 1 deletes the entire existing source tree, so **tag the baseline and push the tag before anything is touched.**

**Files:**
- Create: `.gitignore` (replace), `pom.xml` (replace), `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- Create: `src/main/java/com/criptoativos/CriptoAtivosApplication.java`
- Create: `src/main/resources/application.yml`, `src/test/resources/application-test.yml`
- Create: `src/test/java/com/criptoativos/CriptoAtivosApplicationTest.java`
- Delete: `src/main/java/org/example/` (all of it — `Main.java`, `config/`, `dao/`, `enums/`, `models/`), `.idea/`

**Interfaces:**
- Consumes: nothing.
- Produces: a buildable Maven project on branch `feat/production-revamp`; `com.criptoativos` base package; `./mvnw` works with no global Maven.

- [ ] **Step 1: Clone the repository into a new directory**

The existing `Desktop\CriptoAtivos` folder is the older draft and is not a Git repository — leave it alone.

```powershell
cd C:\Users\MICRO\Desktop
git clone https://github.com/alvarogalhardo/CriptoAtivos.git CriptoAtivos-api
cd C:\Users\MICRO\Desktop\CriptoAtivos-api
git log --oneline
```
Expected: two commits ending at `2dbb72b add files`.

- [ ] **Step 2: Tag and push the baseline**

```powershell
git tag -a v0-academic-baseline -m "Academic version: Maven + Oracle JDBC DAO console application"
git push origin v0-academic-baseline
```

The tag is what lets the README credibly say "this started as coursework — here it is" without keeping the old code in the working tree.

- [ ] **Step 3: Create the working branch**

```powershell
git checkout -b feat/production-revamp
```

- [ ] **Step 4: Replace `.gitignore`**

```gitignore
# Build output
target/
out/
build/
*.class

# Maven
!.mvn/wrapper/maven-wrapper.jar
.mvn/.gradle-enterprise/

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/
.settings/
.classpath
.project

# Secrets — never commit signing keys
src/main/resources/certs/*.pem
.env
.env.*
!.env.example

# OS
.DS_Store
Thumbs.db
```

- [ ] **Step 5: Remove the legacy source tree and IDE files**

```powershell
git rm -r --cached .idea
Remove-Item -Recurse -Force .idea -ErrorAction SilentlyContinue
Remove-Item -Recurse -Force src\main\java\org
New-Item -ItemType Directory -Force src\main\java\com\criptoativos, src\main\resources\db\migration, src\test\java\com\criptoativos\support, src\test\resources
```

- [ ] **Step 6 (optional but recommended): Archive the older local draft as a branch**

The flat console draft in `Desktop\CriptoAtivos` exists nowhere in Git. Preserving it costs one commit and means nothing the owner wrote is ever lost:

```powershell
git checkout --orphan archive/console-draft
git rm -rf . --quiet
Copy-Item -Recurse C:\Users\MICRO\Desktop\CriptoAtivos\src .\src
Copy-Item C:\Users\MICRO\Desktop\CriptoAtivos\.gitignore .\.gitignore
git add -A
git commit -m "chore: archive the original flat console draft for reference"
git push -u origin archive/console-draft
git checkout feat/production-revamp
```

- [ ] **Step 7: Replace `pom.xml`**

The existing one declares `org.example`, Java 24, and an Oracle JDBC driver — all three go. Overwrite it entirely with:

Pin the newest Spring Boot **3.5.x** patch — check https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-parent/versions and substitute it below. Do **not** jump to 4.x; this plan targets the 3.5 API surface.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.0</version>
    <relativePath/>
  </parent>

  <groupId>com.criptoativos</groupId>
  <artifactId>criptoativos-api</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <name>CriptoAtivos API</name>
  <description>Crypto portfolio management REST API</description>

  <properties>
    <java.version>21</java.version>
    <springdoc.version>2.8.6</springdoc.version>
    <spotless.version>2.44.4</spotless.version>
    <java-otp.version>0.4.0</java-otp.version>
  </properties>

  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-oauth2-resource-server</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-actuator</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-cache</artifactId></dependency>
    <dependency><groupId>com.github.ben-manes.caffeine</groupId><artifactId>caffeine</artifactId></dependency>

    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>

    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>${springdoc.version}</version>
    </dependency>

    <!-- RFC 6238 TOTP for two-factor authentication (Task 6) -->
    <dependency>
      <groupId>com.eatthepath</groupId>
      <artifactId>java-otp</artifactId>
      <version>${java-otp.version}</version>
    </dependency>

    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-testcontainers</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>junit-jupiter</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.testcontainers</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId><artifactId>maven-failsafe-plugin</artifactId>
        <executions><execution><goals><goal>integration-test</goal><goal>verify</goal></goals></execution></executions>
      </plugin>
    </plugins>
  </build>
</project>
```

*(JaCoCo and Spotless plugins are added in Task 15 — keep the build fast until the code exists.)*

- [ ] **Step 8: Generate the Maven wrapper**

There is no `mvn` on PATH, so bootstrap the wrapper by hand — download the distribution-agnostic wrapper files from the `maven-wrapper` release and write `.mvn/wrapper/maven-wrapper.properties`:

```properties
wrapperVersion=3.3.2
distributionType=only-script
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip
```

Fetch `mvnw` and `mvnw.cmd` from `https://raw.githubusercontent.com/apache/maven-wrapper/maven-wrapper-3.3.2/maven-wrapper-distribution/src/resources/` into the project root. The `only-script` wrapper needs no committed jar.

Verify:
```powershell
.\mvnw.cmd -v
```
Expected: downloads Maven 3.9.9 on first run, then prints `Apache Maven 3.9.9` and `Java version: 22.0.2`.

- [ ] **Step 9: Write the application class**

`src/main/java/com/criptoativos/CriptoAtivosApplication.java`:
```java
package com.criptoativos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CriptoAtivosApplication {
    public static void main(String[] args) {
        SpringApplication.run(CriptoAtivosApplication.class, args);
    }
}
```

- [ ] **Step 10: Write `application.yml`**

```yaml
spring:
  application:
    name: criptoativos-api
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/criptoativos}
    username: ${DB_USER:criptoativos}
    password: ${DB_PASSWORD:criptoativos}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate          # Flyway owns the schema; Hibernate only checks it
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: ${PORT:8080}
  error:
    include-stacktrace: never

management:
  endpoints.web.exposure.include: health,info,metrics
  endpoint.health.show-details: when_authorized

logging:
  level:
    com.criptoativos: INFO
```

- [ ] **Step 11: Write the context-loads test**

`src/test/java/com/criptoativos/CriptoAtivosApplicationTest.java` — a plain assertion for now; it becomes a real context test in Task 2 once a database exists.

```java
package com.criptoativos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CriptoAtivosApplicationTest {
    @Test
    void applicationClassIsAnnotatedAsSpringBootApplication() {
        assertThat(CriptoAtivosApplication.class)
            .hasAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class);
    }
}
```

- [ ] **Step 12: Run the build**

```powershell
.\mvnw.cmd clean test
```
Expected: `BUILD SUCCESS`, 1 test run, 0 failures.

- [ ] **Step 13: Copy this plan into the repo, commit, and push the branch**

Save this document to `docs/plans/2026-09-10-production-revamp.md`, then:

```powershell
git add -A
git commit -m "chore: replace Oracle JDBC console app with Spring Boot scaffold"
git push -u origin feat/production-revamp
```

---

## Task 2: PostgreSQL, Flyway schema, and the integration-test harness

**Files:**
- Create: `compose.yaml`, `src/main/resources/db/migration/V1__baseline.sql`
- Create: `src/test/java/com/criptoativos/support/AbstractIT.java`
- Create: `src/test/java/com/criptoativos/SchemaMigrationIT.java`
- Modify: `src/test/resources/application-test.yml`

**Interfaces:**
- Consumes: `pom.xml` Testcontainers dependencies (Task 1).
- Produces: `AbstractIT` — the base class every later `*IT` extends; starts one shared Postgres container via `@ServiceConnection`. Tables `users`, `assets`, `wallets`, `holdings`, `operations`.

- [ ] **Step 1: Write `compose.yaml`**

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: criptoativos-db
    environment:
      POSTGRES_DB: criptoativos
      POSTGRES_USER: criptoativos
      POSTGRES_PASSWORD: criptoativos
    ports: ["5432:5432"]
    volumes: ["criptoativos-pgdata:/var/lib/postgresql/data"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U criptoativos -d criptoativos"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  criptoativos-pgdata:
```

*(The `api` service is added in Task 15, once a Dockerfile exists.)*

- [ ] **Step 2: Write `V1__baseline.sql`**

```sql
create extension if not exists "pgcrypto";

create table users (
    id                      uuid primary key default gen_random_uuid(),
    name                    varchar(120) not null,
    email                   varchar(255) not null unique,
    password_hash           varchar(72)  not null,
    cpf                     char(11)     not null unique,
    role                    varchar(20)  not null,
    two_factor_enabled      boolean      not null default false,
    two_factor_secret       varchar(64),
    two_factor_confirmed_at timestamptz,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now()
);

-- Single-use backup codes, stored hashed exactly like passwords (Task 6).
create table user_recovery_codes (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references users (id) on delete cascade,
    code_hash  varchar(72) not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);
create index idx_recovery_codes_user on user_recovery_codes (user_id) where used_at is null;

create table assets (
    id               uuid primary key default gen_random_uuid(),
    asset_type       varchar(31)   not null,
    symbol           varchar(20)   not null unique,
    name             varchar(120)  not null,
    current_price    numeric(19,8) not null,
    daily_change_pct numeric(9,4),
    external_id      varchar(60),
    price_updated_at timestamptz,
    created_at       timestamptz   not null default now(),
    version          bigint        not null default 0,
    constraint chk_assets_price_non_negative check (current_price >= 0)
);

-- Exchange-side available supply: the EstoqueCriptoAtivos concept, made real (Task 8).
-- Kept out of `assets` so the 5-minute price refresh never contends with the trading lock.
create table asset_inventory (
    asset_id           uuid primary key references assets (id) on delete cascade,
    available_quantity numeric(19,8) not null default 0,
    updated_at         timestamptz   not null default now(),
    version            bigint        not null default 0,
    constraint chk_inventory_non_negative check (available_quantity >= 0)
);

create table wallets (
    id           uuid primary key default gen_random_uuid(),
    user_id      uuid not null unique references users (id) on delete cascade,
    cash_balance numeric(19,2) not null default 0,
    created_at   timestamptz   not null default now(),
    version      bigint        not null default 0,
    constraint chk_wallets_balance_non_negative check (cash_balance >= 0)
);

create table holdings (
    id           uuid primary key default gen_random_uuid(),
    wallet_id    uuid not null references wallets (id) on delete cascade,
    asset_id     uuid not null references assets (id),
    quantity     numeric(19,8) not null,
    average_cost numeric(19,8) not null,
    version      bigint        not null default 0,
    constraint uq_holdings_wallet_asset unique (wallet_id, asset_id),
    constraint chk_holdings_quantity_positive check (quantity > 0)
);

create table operations (
    id               uuid primary key default gen_random_uuid(),
    operation_type   varchar(31)   not null,
    user_id          uuid not null references users (id) on delete cascade,
    description      varchar(255)  not null,
    occurred_at      timestamptz   not null default now(),
    -- Transaction subtype
    asset_id         uuid references assets (id),
    transaction_type varchar(10),
    quantity         numeric(19,8),
    unit_price       numeric(19,8),
    total_amount     numeric(19,2),
    -- CashOperation subtype
    cash_type        varchar(10),
    amount           numeric(19,2)
);

create index idx_operations_user_occurred on operations (user_id, occurred_at desc);
create index idx_operations_asset on operations (asset_id);
```

The `check` constraints are the database's own answer to the original `Carteira` bug — even a bug in the service layer cannot drive a balance negative.

- [ ] **Step 3: Write `application-test.yml`**

```yaml
spring:
  jpa:
    hibernate.ddl-auto: validate
    show-sql: false
  flyway:
    clean-disabled: false
logging:
  level:
    org.springframework.test: WARN
```

- [ ] **Step 4: Write `AbstractIT`**

One container reused across the whole suite (`static`, never stopped — Testcontainers' Ryuk reaps it).

```java
package com.criptoativos.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine").withReuse(true);
}
```

- [ ] **Step 5: Write the failing schema test**

`src/test/java/com/criptoativos/SchemaMigrationIT.java`:
```java
package com.criptoativos;

import static org.assertj.core.api.Assertions.assertThat;

import com.criptoativos.support.AbstractIT;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SchemaMigrationIT extends AbstractIT {

    @Autowired DataSource dataSource;

    @Test
    void flywayCreatesEveryDomainTable() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             ResultSet rs = c.getMetaData().getTables(null, "public", "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        assertThat(tables)
            .contains("users", "user_recovery_codes", "assets", "asset_inventory",
                "wallets", "holdings", "operations", "flyway_schema_history");
    }
}
```

- [ ] **Step 6: Run it and watch it fail, then pass**

```powershell
.\mvnw.cmd verify
```
Docker Desktop must be running. Expected on the first attempt (before `V1__baseline.sql` is saved): failure. With the migration in place: `BUILD SUCCESS`, `SchemaMigrationIT` green.

- [ ] **Step 7: Confirm the local stack runs too**

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```
Expected: Flyway logs `Successfully applied 1 migration`, app listens on 8080. Stop with Ctrl+C.

- [ ] **Step 8: Commit**

```powershell
git add -A
git commit -m "feat: add postgres schema, flyway migrations, and testcontainers harness"
```

---

## Task 3: Common layer — errors, money, CPF validation

Build this before any feature, because every later task throws these exceptions and formats money through `Money`.

**Files:**
- Create: `common/Money.java`, `common/ApiExceptionHandler.java`
- Create: `common/exception/{DomainException,NotFoundException,ConflictException,BusinessRuleException}.java`
- Create: `common/validation/{Cpf,CpfValidator}.java`
- Create: `src/test/java/com/criptoativos/common/{MoneyTest,validation/CpfValidatorTest}.java`

**Interfaces:**
- Consumes: nothing.
- Produces:
  - `Money.cash(BigDecimal) : BigDecimal` (scale 2, HALF_EVEN), `Money.units(BigDecimal) : BigDecimal` (scale 8, HALF_EVEN), `Money.ZERO_CASH`
  - `DomainException(HttpStatus status, String title, String detail)` — base for all business failures
  - `NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422)
  - `@Cpf` — Bean Validation constraint for Brazilian CPF, check digits included

- [ ] **Step 1: Write the failing `MoneyTest`**

```java
package com.criptoativos.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void cashRoundsToTwoDecimalPlacesHalfEven() {
        assertThat(Money.cash(new BigDecimal("10.005"))).isEqualByComparingTo("10.00");
        assertThat(Money.cash(new BigDecimal("10.015"))).isEqualByComparingTo("10.02");
        assertThat(Money.cash(new BigDecimal("10.1"))).hasToString("10.10");
    }

    @Test
    void unitsRoundsToEightDecimalPlaces() {
        assertThat(Money.units(new BigDecimal("0.123456785"))).hasToString("0.12345678");
    }

    @Test
    void zeroCashCarriesScaleTwo() {
        assertThat(Money.ZERO_CASH).hasToString("0.00");
    }
}
```

- [ ] **Step 2: Run it, confirm it fails**

```powershell
.\mvnw.cmd test -Dtest=MoneyTest
```
Expected: compilation failure — `cannot find symbol: class Money`.

- [ ] **Step 3: Implement `Money`**

```java
package com.criptoativos.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Single source of truth for monetary scale and rounding. Never use double for money. */
public final class Money {

    public static final int CASH_SCALE = 2;
    public static final int UNIT_SCALE = 8;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO_CASH = BigDecimal.ZERO.setScale(CASH_SCALE);

    private Money() {}

    public static BigDecimal cash(BigDecimal value) {
        return value.setScale(CASH_SCALE, ROUNDING);
    }

    public static BigDecimal units(BigDecimal value) {
        return value.setScale(UNIT_SCALE, ROUNDING);
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
```

- [ ] **Step 4: Run `MoneyTest` — expect PASS**

- [ ] **Step 5: Write the failing `CpfValidatorTest`**

```java
package com.criptoativos.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @ParameterizedTest
    @ValueSource(strings = {"52998224725", "529.982.247-25", "16899535009"})
    void acceptsValidCpf(String cpf) {
        assertThat(validator.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "11111111111", "123", "abcdefghijk", "0000000000"})
    void rejectsInvalidCpf(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @Test
    void nullIsDelegatedToNotNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
```

- [ ] **Step 6: Run it, confirm it fails, then implement**

`common/validation/Cpf.java`:
```java
package com.criptoativos.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CpfValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cpf {
    String message() default "must be a valid CPF";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

`common/validation/CpfValidator.java`:
```java
package com.criptoativos.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // absence is @NotNull's job, not ours
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(digits, 9) == digits.charAt(9)
            && checkDigit(digits, 10) == digits.charAt(10);
    }

    /** Computes the CPF check digit at {@code position} using the standard modulus-11 weighting. */
    private static char checkDigit(String digits, int position) {
        int sum = 0;
        int weight = position + 1;
        for (int i = 0; i < position; i++) {
            sum += (digits.charAt(i) - '0') * weight--;
        }
        int remainder = sum % 11;
        return (char) ('0' + (remainder < 2 ? 0 : 11 - remainder));
    }
}
```

- [ ] **Step 7: Implement the exception hierarchy**

`common/exception/DomainException.java`:
```java
package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected DomainException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}
```

Then three thin subclasses, each following the same shape:
```java
public class NotFoundException extends DomainException {
    public NotFoundException(String detail) { super(HttpStatus.NOT_FOUND, "Resource not found", detail); }
}
public class ConflictException extends DomainException {
    public ConflictException(String detail) { super(HttpStatus.CONFLICT, "Conflict", detail); }
}
public class BusinessRuleException extends DomainException {
    public BusinessRuleException(String detail) { super(HttpStatus.UNPROCESSABLE_ENTITY, "Business rule violated", detail); }
}
```

- [ ] **Step 8: Implement `ApiExceptionHandler`**

```java
package com.criptoativos.common;

import com.criptoativos.common.exception.DomainException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getTitle());
        problem.setType(URI.create("https://criptoativos.dev/errors/" + ex.getClass().getSimpleName()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail problem =
            ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Last resort: log the cause, but never leak it to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal server error");
        return problem;
    }
}
```

- [ ] **Step 9: Run the full suite and commit**

```powershell
.\mvnw.cmd verify
git add -A
git commit -m "feat: add money helpers, problem-detail error handling, and CPF validation"
```

---

## Task 4: User registration

**Files:**
- Create: `user/{User,Role,UserRepository,UserService}.java`
- Create: `user/dto/{RegisterRequest,UserResponse}.java`
- Create: `auth/AuthController.java`
- Create: `src/test/java/com/criptoativos/user/UserServiceTest.java`, `src/test/java/com/criptoativos/auth/RegistrationIT.java`
- Create: `src/test/java/com/criptoativos/support/TestFixtures.java`

**Interfaces:**
- Consumes: `Money.ZERO_CASH`, `ConflictException`, `@Cpf` (Task 3); `AbstractIT` (Task 2).
- Produces:
  - `User` with `getId():UUID`, `getEmail():String`, `getPasswordHash():String`, `getRole():Role`, `getName():String`, `getCpf():String`
  - `Role` enum `{ USER, ADMIN }`
  - `UserRepository extends JpaRepository<User, UUID>` with `Optional<User> findByEmailIgnoreCase(String)`, `boolean existsByEmailIgnoreCase(String)`, `boolean existsByCpf(String)`
  - `UserService.register(RegisterRequest) : User`
  - `POST /api/v1/auth/register` → 201 + `UserResponse`
  - `support/TestFixtures.java` — the shared test helper, grown across later tasks. Its contract, in full:
    ```java
    public final class TestFixtures {
        public static UUID registerUser(String email);                       // Task 4
        public static UUID registerUserWithBalance(BigDecimal balance);      // Task 9 adds the deposit
        public static String userToken();                                    // Task 5
        public static String adminToken();                                   // Task 13
        public static UUID adminId();                                        // Task 13
        public static String currentTotpCode(String base32Secret);           // Task 6
    }
    ```
    Each task implements only the methods it needs; every method is listed here so no task invents a name a later task cannot find.

> **No `UserDetailsService`.** This is a stateless OAuth2 resource server: authentication comes from verifying the JWT signature, not from loading a user on every request. `AuthService` reads the user directly from `UserRepository` at login only.

- [ ] **Step 1: Write the failing `UserServiceTest`** (unit, Mockito, no Spring)

```java
package com.criptoativos.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.criptoativos.common.exception.ConflictException;
import com.criptoativos.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Spy PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @InjectMocks UserService userService;

    private static final RegisterRequest REQUEST =
        new RegisterRequest("Ana Maria", "ana@example.com", "s3cret-passw0rd", "52998224725");

    @Test
    void registerHashesThePasswordAndNeverStoresItInPlaintext() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.register(REQUEST);

        assertThat(created.getPasswordHash()).isNotEqualTo("s3cret-passw0rd").startsWith("$2");
        assertThat(passwordEncoder.matches("s3cret-passw0rd", created.getPasswordHash())).isTrue();
        assertThat(created.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void registerStripsCpfFormatting() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.register(
            new RegisterRequest("Ana Maria", "ana@example.com", "s3cret-passw0rd", "529.982.247-25"));

        assertThat(created.getCpf()).isEqualTo("52998224725");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(REQUEST))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    void registerCreatesAWalletWithAZeroBalance() {
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByCpf(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.register(REQUEST);

        assertThat(created.getWallet()).isNotNull();
        assertThat(created.getWallet().getCashBalance()).isEqualByComparingTo("0.00");
    }
}
```

- [ ] **Step 2: Run it, confirm compilation failure**

```powershell
.\mvnw.cmd test -Dtest=UserServiceTest
```

- [ ] **Step 3: Implement `Role` and `User`**

`user/Role.java`:
```java
package com.criptoativos.user;

public enum Role {
    USER,
    ADMIN
}
```

`user/User.java` — note the `Wallet` is created together with the user, so a registered account can never be walletless (the original `Main` had this coupling implicit and fragile):
```java
package com.criptoativos.user;

import com.criptoativos.wallet.Wallet;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(nullable = false, columnDefinition = "char(11)")
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Wallet wallet;

    protected User() {} // JPA

    private User(String name, String email, String passwordHash, String cpf, Role role) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.cpf = cpf;
        this.role = role;
    }

    /** Creates a user together with the empty wallet that every account must have. */
    public static User create(String name, String email, String passwordHash, String cpf, Role role) {
        User user = new User(name, email, passwordHash, cpf, role);
        user.wallet = Wallet.forUser(user);
        return user;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getCpf() { return cpf; }
    public Role getRole() { return role; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Wallet getWallet() { return wallet; }
}
```

> **Ordering note:** `Wallet` is written in Task 9. To keep Task 4 independently compilable and testable, create `wallet/Wallet.java` here with only `forUser(User)`, `getCashBalance()`, `getId()`, and its JPA mapping; Task 9 adds the behaviour. Do the same for `WalletRepository`.

- [ ] **Step 4: Implement `UserRepository`**

```java
package com.criptoativos.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByCpf(String cpf);
}
```

- [ ] **Step 5: Implement `RegisterRequest` and `UserResponse`**

`user/dto/RegisterRequest.java`:
```java
package com.criptoativos.user.dto;

import com.criptoativos.common.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 12, max = 100) String password,
    @NotBlank @Cpf String cpf) {}
```

`user/dto/UserResponse.java` — deliberately omits `passwordHash` and `cpf`:
```java
package com.criptoativos.user.dto;

import com.criptoativos.user.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, String role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.getCreatedAt());
    }
}
```

- [ ] **Step 6: Implement `UserService`**

```java
package com.criptoativos.user;

import com.criptoativos.common.exception.ConflictException;
import com.criptoativos.common.exception.NotFoundException;
import com.criptoativos.user.dto.RegisterRequest;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        String cpf = request.cpf().replaceAll("\\D", "");
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email is already registered.");
        }
        if (userRepository.existsByCpf(cpf)) {
            throw new ConflictException("CPF is already registered.");
        }
        User user = User.create(
            request.name().trim(),
            request.email().toLowerCase(),
            passwordEncoder.encode(request.password()),
            cpf,
            Role.USER);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User requireById(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(id)));
    }

    @Transactional(readOnly = true)
    public User requireByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(email)));
    }
}
```

- [ ] **Step 7: Run `UserServiceTest` — expect PASS**

- [ ] **Step 8: Add `AuthController` with the register endpoint**

```java
package com.criptoativos.auth;

import com.criptoativos.user.UserService;
import com.criptoativos.user.dto.RegisterRequest;
import com.criptoativos.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse body = UserResponse.from(userService.register(request));
        return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
    }
}
```

- [ ] **Step 9: Add a temporary permissive `SecurityConfig`**

Spring Security's default locks everything; Task 5 replaces this file wholesale.

```java
package com.criptoativos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}
```

- [ ] **Step 10: Write `RegistrationIT`**

```java
package com.criptoativos.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.criptoativos.support.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RegistrationIT extends AbstractIT {

    @Autowired MockMvc mockMvc;

    private static final String VALID = """
        {"name":"Ana Maria","email":"ana@example.com","password":"s3cret-passw0rd","cpf":"529.982.247-25"}
        """;

    @Test
    void registersAUserAndReturns201WithoutLeakingCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.name").value("Ana Maria"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    void rejectsAnInvalidCpfWithAProblemDetail() throws Exception {
        String body = VALID.replace("529.982.247-25", "111.111.111-11");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"))
            .andExpect(jsonPath("$.errors.cpf").value("must be a valid CPF"));
    }

    @Test
    void rejectsADuplicateEmailWith409() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID));
        String body = VALID.replace("529.982.247-25", "168.995.350-09");
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("Email is already registered."));
    }
}
```

> Add `@Transactional` to `RegistrationIT` so each test rolls back, keeping the shared container clean between tests.

- [ ] **Step 11: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add user registration with bcrypt hashing and CPF validation"
```

---

## Task 5: Spring Security + JWT authentication

**Files:**
- Create: `config/JwtConfig.java`, `auth/TokenService.java`, `auth/AuthService.java`, `common/SecurityUtils.java`
- Create: `auth/dto/{LoginRequest,TokenResponse}.java`
- Create: `user/UserController.java`, `user/dto/UpdateProfileRequest.java`
- Create: `src/main/resources/certs/.gitkeep`
- Modify: `config/SecurityConfig.java` (replace the permissive stub), `auth/AuthController.java`, `application.yml`
- Create: `src/test/java/com/criptoativos/auth/AuthenticationIT.java`

**Interfaces:**
- Consumes: `UserService.requireByEmail(String)`, `User`, `Role` (Task 4).
- Produces:
  - `TokenService.generateToken(User) : String` — signed RS256 JWT, `sub` = user id, claim `email`, claim `scope` = role name, 2-hour expiry
  - `AuthService.login(LoginRequest) : TokenResponse`
  - `POST /api/v1/auth/login` → 200 + `TokenResponse(String accessToken, String tokenType, long expiresIn)`
  - `GET /api/v1/users/me`, `PUT /api/v1/users/me`
  - `SecurityUtils.currentUserId() : UUID` — used by every later controller
  - Authorities are `ROLE_USER` / `ROLE_ADMIN`, so `@PreAuthorize("hasRole('ADMIN')")` works in Tasks 7, 8, and 13.

- [ ] **Step 1: Generate the RSA signing keypair**

Run in **Git Bash** (ships with OpenSSL):

```bash
cd /c/Users/MICRO/Desktop/CriptoAtivos
mkdir -p src/main/resources/certs
openssl genrsa -out src/main/resources/certs/private.pem 2048
openssl rsa -in src/main/resources/certs/private.pem -pubout -out src/main/resources/certs/public.pem
```

`.gitignore` (Task 1) already excludes `src/main/resources/certs/*.pem`. Commit `certs/.gitkeep` so the directory survives, and document key generation in the README (Task 16).

- [ ] **Step 2: Add JWT config to `application.yml`**

```yaml
app:
  security:
    jwt:
      public-key: ${JWT_PUBLIC_KEY:classpath:certs/public.pem}
      private-key: ${JWT_PRIVATE_KEY:classpath:certs/private.pem}
      ttl: PT2H
      issuer: criptoativos-api
```

- [ ] **Step 3: Write the failing `AuthenticationIT`**

```java
package com.criptoativos.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.criptoativos.support.AbstractIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AuthenticationIT extends AbstractIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void registerUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"Ana Maria","email":"ana@example.com","password":"s3cret-passw0rd","cpf":"52998224725"}
                """));
    }

    private String login(String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("accessToken").asText();
    }

    @Test
    void loginReturnsAUsableBearerToken() throws Exception {
        String token = login("s3cret-passw0rd");

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void loginWithAWrongPasswordReturns401AndRevealsNothing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@example.com\",\"password\":\"wrong-password-x\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void loginWithAnUnknownEmailReturnsTheSameMessageAsAWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"s3cret-passw0rd\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value("Invalid email or password."));
    }

    @Test
    void protectedEndpointsRejectAnAbsentToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRejectAGarbageToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
            .andExpect(status().isUnauthorized());
    }
}
```

Note the third test: identical messages for unknown-email and wrong-password prevent user enumeration.

- [ ] **Step 4: Run it, confirm it fails**

- [ ] **Step 5: Implement `JwtConfig`**

Spring Boot converts `classpath:*.pem` resources into `RSAPublicKey`/`RSAPrivateKey` automatically via `RsaKeyConverters`.

```java
package com.criptoativos.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;

@Configuration
@EnableConfigurationProperties(JwtConfig.JwtProperties.class)
public class JwtConfig {

    @ConfigurationProperties("app.security.jwt")
    public record JwtProperties(
        RSAPublicKey publicKey, RSAPrivateKey privateKey, Duration ttl, String issuer) {}

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withPublicKey(properties.publicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        JWK jwk = new RSAKey.Builder(properties.publicKey()).privateKey(properties.privateKey()).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new com.nimbusds.jose.jwk.JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}
```

- [ ] **Step 6: Implement `TokenService`**

```java
package com.criptoativos.auth;

import com.criptoativos.config.JwtConfig.JwtProperties;
import com.criptoativos.user.User;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .issuedAt(now)
            .expiresAt(now.plus(properties.ttl()))
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("scope", user.getRole().name())
            .build();
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return properties.ttl().toSeconds();
    }
}
```

- [ ] **Step 7: Implement `AuthService` and the DTOs**

`auth/dto/LoginRequest.java`:
```java
package com.criptoativos.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
```

`auth/dto/TokenResponse.java`:
```java
package com.criptoativos.auth.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static TokenResponse bearer(String token, long expiresIn) {
        return new TokenResponse(token, "Bearer", expiresIn);
    }
}
```

`auth/AuthService.java` — the encoder is invoked even when the email is unknown, so response timing does not leak account existence:
```java
package com.criptoativos.auth;

import com.criptoativos.auth.dto.LoginRequest;
import com.criptoativos.auth.dto.TokenResponse;
import com.criptoativos.user.User;
import com.criptoativos.user.UserRepository;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** A well-formed BCrypt hash of a value nobody can supply, used to equalise timing. */
    private static final String DUMMY_HASH =
        "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5XcJ5lB0mQ5sYb3wXqzQ5t1n0uK1G";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Optional<User> candidate = userRepository.findByEmailIgnoreCase(request.email());
        String hash = candidate.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean matches = passwordEncoder.matches(request.password(), hash);

        if (candidate.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password.");
        }
        return TokenResponse.bearer(tokenService.generateToken(candidate.get()), tokenService.ttlSeconds());
    }
}
```

- [ ] **Step 8: Replace `SecurityConfig`**

```java
package com.criptoativos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("ROLE_");   // scope "ADMIN" -> authority ROLE_ADMIN
        authorities.setAuthoritiesClaimName("scope");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter converter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())                 // stateless bearer-token API, no cookies
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/assets", "/api/v1/assets/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
            .build();
    }
}
```

- [ ] **Step 9: Add `BadCredentialsException` handling to `ApiExceptionHandler`**

```java
@ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
ProblemDetail handleBadCredentials(org.springframework.security.authentication.BadCredentialsException ex) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    problem.setTitle("Authentication failed");
    return problem;
}
```

- [ ] **Step 10: Add `SecurityUtils` and `UserController`**

`common/SecurityUtils.java`:
```java
package com.criptoativos.common;

import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {}

    /** The authenticated user's id, taken from the JWT subject. */
    public static UUID currentUserId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }
}
```

`user/UserController.java` exposes `GET /api/v1/users/me` returning `UserResponse.from(userService.requireById(SecurityUtils.currentUserId()))`, and `PUT /api/v1/users/me` accepting `UpdateProfileRequest(@NotBlank @Size(max=120) String name)` and calling `user.rename(...)`.

- [ ] **Step 11: Add the login endpoint to `AuthController`**

```java
@PostMapping("/login")
public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
}
```

- [ ] **Step 12: Run `./mvnw verify` — all `AuthenticationIT` tests pass. Commit.**

```powershell
git add -A
git commit -m "feat: add JWT authentication with RS256 signing and role-based authorization"
```

---

## Task 6: Two-factor authentication (TOTP)

The remote version had a 2FA menu entry backed by a plain boolean. This turns it into a real RFC 6238 flow: enrol → confirm → challenge on login → verify, with single-use recovery codes.

**Files:**
- Create: `auth/twofactor/{TotpService,TwoFactorService,TwoFactorController,RecoveryCode,RecoveryCodeRepository}.java`
- Create: `auth/twofactor/dto/{TwoFactorSetupResponse,TwoFactorCodeRequest,TwoFactorVerifyRequest,RecoveryCodesResponse}.java`
- Create: `config/EncryptionConfig.java`
- Modify: `auth/AuthService.java`, `auth/TokenService.java`, `auth/dto/TokenResponse.java` → `LoginResponse`, `config/SecurityConfig.java`, `user/User.java`, `application.yml`
- Create: `src/test/java/com/criptoativos/auth/twofactor/{TotpServiceTest,TwoFactorIT}.java`

**Interfaces:**
- Consumes: `TokenService.generateToken(User)`, `SecurityUtils.currentUserId()`, `PasswordEncoder` (Task 5); `User` (Task 4).
- Produces:
  - `TotpService.generateSecret() : String` (Base32), `.buildProvisioningUri(String email, String secret) : String`, `.verify(String secret, String code) : boolean` (±1 time step of drift)
  - `TokenService.generateChallengeToken(User) : String` — 5-minute TTL, `scope` claim `2FA_CHALLENGE`
  - `TwoFactorService.beginSetup(UUID) : TwoFactorSetupResponse`, `.confirm(UUID, String code) : RecoveryCodesResponse`, `.disable(UUID, String code)`, `.verifyChallenge(UUID, String code) : boolean`
  - `LoginResponse(boolean twoFactorRequired, String challengeToken, String accessToken, String tokenType, Long expiresIn)` — replaces `TokenResponse` as the `/auth/login` body
  - Endpoints: `POST /api/v1/auth/2fa/setup`, `POST /api/v1/auth/2fa/confirm`, `POST /api/v1/auth/2fa/disable`, `POST /api/v1/auth/2fa/verify`

- [ ] **Step 1: Add configuration and the encryption bean**

TOTP secrets are password-equivalent, so they are encrypted at rest rather than stored in the clear.

```yaml
app:
  security:
    twofactor:
      issuer: CriptoAtivos
      challenge-ttl: PT5M
      recovery-code-count: 10
    encryption:
      password: ${ENCRYPTION_PASSWORD:dev-only-change-me}
      salt: ${ENCRYPTION_SALT:5c0744940b5c369b}   # 16 hex chars
```

`config/EncryptionConfig.java`:
```java
package com.criptoativos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {

    /** AES-256 for TOTP secrets at rest, so a database dump alone cannot mint valid codes. */
    @Bean
    TextEncryptor textEncryptor(
        @Value("${app.security.encryption.password}") String password,
        @Value("${app.security.encryption.salt}") String salt) {
        return Encryptors.delux(password, salt);
    }
}
```

- [ ] **Step 2: Write the failing `TotpServiceTest`**

```java
package com.criptoativos.auth.twofactor;

import static org.assertj.core.api.Assertions.assertThat;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private final TotpService totpService = new TotpService("CriptoAtivos");

    @Test
    void generatesADistinctBase32SecretEachTime() {
        String first = totpService.generateSecret();
        String second = totpService.generateSecret();

        assertThat(first).hasSize(32).matches("[A-Z2-7]+");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void acceptsTheCurrentCode() throws Exception {
        String secret = totpService.generateSecret();
        assertThat(totpService.verify(secret, currentCode(secret, Instant.now()))).isTrue();
    }

    @Test
    void acceptsACodeOneStepInThePastToToleratePhoneClockDrift() throws Exception {
        String secret = totpService.generateSecret();
        String previous = currentCode(secret, Instant.now().minusSeconds(30));

        assertThat(totpService.verify(secret, previous)).isTrue();
    }

    @Test
    void rejectsACodeThatIsTooOld() throws Exception {
        String secret = totpService.generateSecret();
        String stale = currentCode(secret, Instant.now().minusSeconds(180));

        assertThat(totpService.verify(secret, stale)).isFalse();
    }

    @Test
    void rejectsGarbageWithoutThrowing() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verify(secret, "000000")).isFalse();
        assertThat(totpService.verify(secret, "not-a-code")).isFalse();
        assertThat(totpService.verify(secret, "")).isFalse();
        assertThat(totpService.verify(secret, null)).isFalse();
    }

    @Test
    void buildsAScannableProvisioningUri() {
        String uri = totpService.buildProvisioningUri("ana@example.com", "JBSWY3DPEHPK3PXP");

        assertThat(uri)
            .startsWith("otpauth://totp/CriptoAtivos:ana%40example.com")
            .contains("secret=JBSWY3DPEHPK3PXP")
            .contains("issuer=CriptoAtivos")
            .contains("algorithm=SHA1")
            .contains("digits=6")
            .contains("period=30");
    }

    private static String currentCode(String base32Secret, Instant at) throws Exception {
        var generator = new TimeBasedOneTimePasswordGenerator();
        var key = new SecretKeySpec(new Base32().decode(base32Secret), "HmacSHA1");
        return "%06d".formatted(generator.generateOneTimePassword(key, at));
    }
}
```

`Base32` comes from `commons-codec`, already on the classpath transitively via Spring; if `./mvnw dependency:tree` shows it absent, add `commons-codec:commons-codec` explicitly.

- [ ] **Step 3: Run it, confirm it fails**

```powershell
.\mvnw.cmd test -Dtest=TotpServiceTest
```

- [ ] **Step 4: Implement `TotpService`**

```java
package com.criptoativos.auth.twofactor;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private static final int SECRET_BYTES = 20;      // 160 bits, per RFC 4226
    private static final int DRIFT_STEPS = 1;        // accept ±30s
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TimeBasedOneTimePasswordGenerator generator = new TimeBasedOneTimePasswordGenerator();
    private final String issuer;

    public TotpService(@Value("${app.security.twofactor.issuer}") String issuer) {
        this.issuer = issuer;
    }

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return new Base32().encodeToString(bytes).replace("=", "");
    }

    public boolean verify(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        try {
            var key = new SecretKeySpec(new Base32().decode(base32Secret), "HmacSHA1");
            Instant now = Instant.now();
            for (int step = -DRIFT_STEPS; step <= DRIFT_STEPS; step++) {
                Instant at = now.plusSeconds(step * generator.getTimeStep().toSeconds());
                if (constantTimeEquals("%06d".formatted(generator.generateOneTimePassword(key, at)), code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;   // malformed secret or code is a failed verification, never a 500
        }
    }

    public String buildProvisioningUri(String email, String base32Secret) {
        String label = encode(issuer + ":" + email);
        return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30"
            .formatted(label, base32Secret, encode(issuer));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Comparison that does not leak how many leading digits were correct. */
    private static boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 5: Run `TotpServiceTest` — expect PASS**

- [ ] **Step 6: Add the 2FA fields to `User`**

```java
@Column(name = "two_factor_secret", length = 64)
private String twoFactorSecret;              // AES-encrypted ciphertext, never the raw Base32

@Column(name = "two_factor_confirmed_at")
private Instant twoFactorConfirmedAt;

public void stageTwoFactorSecret(String encryptedSecret) {
    this.twoFactorSecret = encryptedSecret;
    this.twoFactorEnabled = false;           // not active until a code is confirmed
    this.twoFactorConfirmedAt = null;
}

public void confirmTwoFactor() {
    this.twoFactorEnabled = true;
    this.twoFactorConfirmedAt = Instant.now();
}

public void disableTwoFactor() {
    this.twoFactorEnabled = false;
    this.twoFactorSecret = null;
    this.twoFactorConfirmedAt = null;
}

public String getTwoFactorSecret() { return twoFactorSecret; }
```

- [ ] **Step 7: Implement `RecoveryCode` and its repository**

Entity over `user_recovery_codes` with `user`, `codeHash`, `usedAt`. `static RecoveryCode issue(User user, String hash)` and `void markUsed()`.

```java
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {
    List<RecoveryCode> findByUserIdAndUsedAtIsNull(UUID userId);
    void deleteByUserId(UUID userId);
}
```

- [ ] **Step 8: Implement `TwoFactorService`**

```java
@Service
public class TwoFactorService {

    private static final int CODE_GROUPS = 2;
    private static final int GROUP_LENGTH = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no I/O/0/1

    private final UserRepository userRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final TotpService totpService;
    private final TextEncryptor encryptor;
    private final PasswordEncoder passwordEncoder;
    private final int recoveryCodeCount;

    // constructor injection omitted for brevity

    @Transactional
    public TwoFactorSetupResponse beginSetup(UUID userId) {
        User user = requireUser(userId);
        if (user.isTwoFactorEnabled()) {
            throw new ConflictException("Two-factor authentication is already enabled.");
        }
        String secret = totpService.generateSecret();
        user.stageTwoFactorSecret(encryptor.encrypt(secret));
        return new TwoFactorSetupResponse(secret, totpService.buildProvisioningUri(user.getEmail(), secret));
    }

    @Transactional
    public RecoveryCodesResponse confirm(UUID userId, String code) {
        User user = requireUser(userId);
        if (user.getTwoFactorSecret() == null) {
            throw new BusinessRuleException("Start two-factor setup before confirming.");
        }
        if (!totpService.verify(encryptor.decrypt(user.getTwoFactorSecret()), code)) {
            throw new BusinessRuleException("Invalid verification code.");
        }
        user.confirmTwoFactor();
        return new RecoveryCodesResponse(regenerateRecoveryCodes(user));
    }

    @Transactional
    public void disable(UUID userId, String code) {
        User user = requireUser(userId);
        if (!user.isTwoFactorEnabled() || !verifyAnyFactor(user, code)) {
            throw new BusinessRuleException("Invalid verification code.");
        }
        user.disableTwoFactor();
        recoveryCodeRepository.deleteByUserId(userId);
    }

    /** Accepts either a live TOTP code or an unused recovery code, consuming the latter. */
    @Transactional
    public boolean verifyAnyFactor(User user, String code) {
        if (user.getTwoFactorSecret() != null
            && totpService.verify(encryptor.decrypt(user.getTwoFactorSecret()), code)) {
            return true;
        }
        for (RecoveryCode candidate : recoveryCodeRepository.findByUserIdAndUsedAtIsNull(user.getId())) {
            if (passwordEncoder.matches(code, candidate.getCodeHash())) {
                candidate.markUsed();
                return true;
            }
        }
        return false;
    }

    private List<String> regenerateRecoveryCodes(User user) {
        recoveryCodeRepository.deleteByUserId(user.getId());
        List<String> plaintext = new ArrayList<>();
        for (int i = 0; i < recoveryCodeCount; i++) {
            String code = randomCode();
            plaintext.add(code);
            recoveryCodeRepository.save(RecoveryCode.issue(user, passwordEncoder.encode(code)));
        }
        return plaintext;   // shown exactly once, never retrievable again
    }

    private static String randomCode() {
        StringBuilder builder = new StringBuilder();
        for (int group = 0; group < CODE_GROUPS; group++) {
            if (group > 0) {
                builder.append('-');
            }
            for (int i = 0; i < GROUP_LENGTH; i++) {
                builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return builder.toString();
    }
}
```

- [ ] **Step 9: Add the challenge token and rework the login response**

In `TokenService`:
```java
public static final String CHALLENGE_SCOPE = "2FA_CHALLENGE";

public String generateChallengeToken(User user) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .issuedAt(now)
        .expiresAt(now.plus(challengeTtl))
        .subject(user.getId().toString())
        .claim("scope", CHALLENGE_SCOPE)
        .build();
    return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
}
```

`auth/dto/LoginResponse.java`:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
    boolean twoFactorRequired, String challengeToken,
    String accessToken, String tokenType, Long expiresIn) {

    public static LoginResponse accessGranted(String token, long expiresIn) {
        return new LoginResponse(false, null, token, "Bearer", expiresIn);
    }

    public static LoginResponse challenge(String challengeToken) {
        return new LoginResponse(true, challengeToken, null, null, null);
    }
}
```

In `AuthService.login`, after the credentials check:
```java
User user = candidate.get();
return user.isTwoFactorEnabled()
    ? LoginResponse.challenge(tokenService.generateChallengeToken(user))
    : LoginResponse.accessGranted(tokenService.generateToken(user), tokenService.ttlSeconds());
```

- [ ] **Step 10: Close the challenge-token privilege hole in `SecurityConfig`**

A challenge token carries `scope: 2FA_CHALLENGE`, which becomes authority `ROLE_2FA_CHALLENGE`. Under the Task 5 rule `anyRequest().authenticated()` **that token would open every protected endpoint** — a half-authenticated session with full access. Replace the catch-all with explicit role requirements:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
    .requestMatchers("/api/v1/auth/2fa/verify").hasRole("2FA_CHALLENGE")   // only a challenge token
    .requestMatchers(HttpMethod.GET, "/api/v1/assets", "/api/v1/assets/**").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
    .anyRequest().hasAnyRole("USER", "ADMIN"))                              // never 2FA_CHALLENGE
```

- [ ] **Step 11: Implement `TwoFactorController`**

`POST /2fa/setup` and `/2fa/confirm` and `/2fa/disable` run as the authenticated user via `SecurityUtils.currentUserId()`. `POST /2fa/verify` reads the subject from the challenge token and returns a full `LoginResponse.accessGranted(...)` once `TwoFactorService.verifyAnyFactor` passes; otherwise it throws `BadCredentialsException("Invalid verification code.")`.

- [ ] **Step 12: Write `TwoFactorIT`**

Cover, end to end: setup returns a scannable URI and does **not** yet enable 2FA; confirming with a wrong code fails and 2FA stays off; confirming with a real code enables it and returns 10 recovery codes; login now returns `twoFactorRequired: true` with a challenge token and **no** access token; the challenge token is rejected by `GET /api/v1/users/me` with 403; `POST /2fa/verify` with a valid code returns a working access token; a recovery code works once and is rejected the second time; disabling 2FA restores plain login.

Generate live codes in the test with the same `currentCode` helper from `TotpServiceTest` — promote it into `support/TestFixtures.java`.

- [ ] **Step 13: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add TOTP two-factor authentication with recovery codes"
```

---

## Task 7: Assets

**Files:**
- Create: `asset/{Asset,CryptoAsset,AssetRepository,AssetService,AssetController}.java`
- Create: `asset/dto/{CreateCryptoAssetRequest,AssetResponse}.java`
- Create: `src/main/resources/db/migration/V2__seed_assets.sql`
- Create: `src/test/java/com/criptoativos/asset/AssetIT.java`

**Interfaces:**
- Consumes: `NotFoundException`, `ConflictException`, `Money.units` (Task 3); `ROLE_ADMIN` authority (Task 5).
- Produces:
  - `Asset` (abstract, `@Inheritance(SINGLE_TABLE)`, discriminator column `asset_type`) with `getId():UUID`, `getSymbol():String`, `getName():String`, `getCurrentPrice():BigDecimal`, `updatePrice(BigDecimal newPrice, BigDecimal dailyChangePct)`, abstract `String category()`
  - `CryptoAsset extends Asset` with `getDailyChangePct():BigDecimal`, `getExternalId():String`, `getPriceUpdatedAt():Instant`, static `CryptoAsset.create(String symbol, String name, BigDecimal price, String externalId)`
  - `AssetRepository extends JpaRepository<Asset, UUID>` with `Optional<Asset> findBySymbolIgnoreCase(String)`, `boolean existsBySymbolIgnoreCase(String)`, `List<CryptoAsset> findAllByExternalIdNotNull()`
  - `AssetService.requireBySymbol(String) : Asset`, `.list(Pageable) : Page<Asset>`, `.createCrypto(CreateCryptoAssetRequest) : CryptoAsset`, `.updatePrice(String symbol, BigDecimal price) : Asset`
  - `GET /api/v1/assets` (public, paged), `GET /api/v1/assets/{symbol}` (public), `POST /api/v1/assets` (ADMIN), `PATCH /api/v1/assets/{symbol}/price` (ADMIN)

> `updatePrice` is a real admin capability, not a test hook: it is what lets the API run with `PRICE_PROVIDER=manual` and no network. Tasks 10 and 11 both depend on it. Implement it as `@Transactional`, resolving via `requireBySymbol` and delegating to `Asset.updatePrice(price, null)`.

- [ ] **Step 1: Write the failing `AssetIT`**

Cover: seeded assets are listed; lookup by symbol is case-insensitive; unknown symbol → 404 ProblemDetail; `POST` without a token → 401; `POST` with a USER token → 403; `POST` with an ADMIN token → 201; duplicate symbol → 409.

```java
package com.criptoativos.asset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.criptoativos.support.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AssetIT extends AbstractIT {

    @Autowired MockMvc mockMvc;

    private static final String NEW_ASSET =
        "{\"symbol\":\"SOL\",\"name\":\"Solana\",\"currentPrice\":\"142.50\",\"externalId\":\"solana\"}";

    @Test
    void listsSeededAssetsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/assets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].symbol").value(org.hamcrest.Matchers.hasItem("BTC")));
    }

    @Test
    void looksUpBySymbolCaseInsensitively() throws Exception {
        mockMvc.perform(get("/api/v1/assets/btc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("BTC"))
            .andExpect(jsonPath("$.category").value("CRYPTO"));
    }

    @Test
    void returns404ProblemDetailForAnUnknownSymbol() throws Exception {
        mockMvc.perform(get("/api/v1/assets/NOPE"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void rejectsAssetCreationWithoutAToken() throws Exception {
        mockMvc.perform(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content(NEW_ASSET))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void rejectsAssetCreationForANonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content(NEW_ASSET))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void allowsAnAdminToCreateAnAsset() throws Exception {
        mockMvc.perform(post("/api/v1/assets").contentType(MediaType.APPLICATION_JSON).content(NEW_ASSET))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.symbol").value("SOL"));
    }
}
```

- [ ] **Step 2: Run it, confirm failure**

- [ ] **Step 3: Implement `Asset`**

```java
package com.criptoativos.asset;

import com.criptoativos.common.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "asset_type", discriminatorType = DiscriminatorType.STRING, length = 31)
public abstract class Asset {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "current_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal currentPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version private long version;

    protected Asset() {}

    protected Asset(String symbol, String name, BigDecimal currentPrice) {
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.currentPrice = Money.units(currentPrice);
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /** Replaces the fake {@code calcularVariacaoDiaria()} of the original design with real data. */
    public abstract void updatePrice(BigDecimal newPrice, BigDecimal dailyChangePct);

    /** Discriminator exposed to clients, e.g. "CRYPTO". */
    public abstract String category();

    protected void setCurrentPrice(BigDecimal price) {
        this.currentPrice = Money.units(price);
    }

    public UUID getId() { return id; }
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public Instant getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 4: Implement `CryptoAsset`**

```java
package com.criptoativos.asset;

import com.criptoativos.common.Money;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@DiscriminatorValue("CRYPTO")
public class CryptoAsset extends Asset {

    @Column(name = "daily_change_pct", precision = 9, scale = 4)
    private BigDecimal dailyChangePct;

    /** Provider-side identifier, e.g. the CoinGecko coin id "bitcoin". */
    @Column(name = "external_id", length = 60)
    private String externalId;

    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    protected CryptoAsset() {}

    private CryptoAsset(String symbol, String name, BigDecimal price, String externalId) {
        super(symbol, name, price);
        this.externalId = externalId;
    }

    public static CryptoAsset create(String symbol, String name, BigDecimal price, String externalId) {
        return new CryptoAsset(symbol, name, price, externalId);
    }

    @Override
    public void updatePrice(BigDecimal newPrice, BigDecimal dailyChangePct) {
        setCurrentPrice(newPrice);
        this.dailyChangePct = dailyChangePct == null ? null : dailyChangePct.setScale(4, Money.ROUNDING);
        this.priceUpdatedAt = Instant.now();
    }

    @Override
    public String category() {
        return "CRYPTO";
    }

    public BigDecimal getDailyChangePct() { return dailyChangePct; }
    public String getExternalId() { return externalId; }
    public Instant getPriceUpdatedAt() { return priceUpdatedAt; }
}
```

- [ ] **Step 5: Implement repository, DTOs, service, controller**

`AssetResponse` is a record with `(UUID id, String symbol, String name, String category, BigDecimal currentPrice, BigDecimal dailyChangePct, Instant priceUpdatedAt)` and a `static AssetResponse from(Asset asset)` that reads the crypto-only fields with `instanceof CryptoAsset c`.

`AssetService.createCrypto` throws `ConflictException` when `existsBySymbolIgnoreCase`; `requireBySymbol` throws `NotFoundException("Asset SYMBOL does not exist.")`.

`AssetController`:
```java
@GetMapping
public Page<AssetResponse> list(@PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
    return assetService.list(pageable).map(AssetResponse::from);
}

@GetMapping("/{symbol}")
public AssetResponse bySymbol(@PathVariable String symbol) {
    return AssetResponse.from(assetService.requireBySymbol(symbol));
}

@PostMapping
@PreAuthorize("hasRole('ADMIN')")
@ResponseStatus(HttpStatus.CREATED)
public AssetResponse create(@Valid @RequestBody CreateCryptoAssetRequest request) {
    return AssetResponse.from(assetService.createCrypto(request));
}
```

- [ ] **Step 6: Write `V2__seed_assets.sql`**

```sql
insert into assets (asset_type, symbol, name, current_price, external_id) values
    ('CRYPTO', 'BTC',  'Bitcoin',  0, 'bitcoin'),
    ('CRYPTO', 'ETH',  'Ethereum', 0, 'ethereum'),
    ('CRYPTO', 'SOL',  'Solana',   0, 'solana'),
    ('CRYPTO', 'ADA',  'Cardano',  0, 'cardano'),
    ('CRYPTO', 'XRP',  'XRP',      0, 'ripple')
on conflict (symbol) do nothing;
```

Prices start at 0 and are filled by the Task 9 refresh job on first run.

> `AssetIT.allowsAnAdminToCreateAnAsset` posts `SOL`, which this migration also seeds. Change the test's payload to a symbol not in the seed (e.g. `DOT` / `polkadot`) so the two don't collide, and add a separate test asserting `409` when posting `BTC`.

- [ ] **Step 7: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add asset catalogue with JPA inheritance and admin-only creation"
```

---

## Task 8: Asset inventory (exchange-side supply)

`EstoqueCriptoAtivos` in the remote version, made real: the exchange holds a finite quantity of each asset, buys draw it down, and sells return it. Without this, the API silently assumes infinite liquidity.

**Files:**
- Create: `asset/inventory/{AssetInventory,AssetInventoryRepository,AssetInventoryService,AssetInventoryController}.java`
- Create: `asset/inventory/dto/{InventoryResponse,RestockRequest}.java`
- Create: `common/exception/InsufficientInventoryException.java`
- Modify: `asset/AssetService.java` (create inventory alongside a new asset), `asset/dto/AssetResponse.java`, `src/main/resources/db/migration/V2__seed_assets.sql`
- Create: `src/test/java/com/criptoativos/asset/inventory/AssetInventoryIT.java`

**Interfaces:**
- Consumes: `Asset`, `AssetService.requireBySymbol` (Task 7); `Money.units`, `BusinessRuleException` (Task 3); `ROLE_ADMIN` (Task 5).
- Produces:
  - `AssetInventory` with `getAvailableQuantity():BigDecimal`, `reserve(BigDecimal)`, `release(BigDecimal)`, `restock(BigDecimal)`
  - `AssetInventoryRepository.findByAssetIdForUpdate(UUID) : Optional<AssetInventory>` — `@Lock(PESSIMISTIC_WRITE)`, consumed by Task 10
  - `AssetInventoryService.reserve(Asset, BigDecimal)`, `.release(Asset, BigDecimal)`, `.restock(String symbol, BigDecimal) : AssetInventory`, `.available(String symbol) : BigDecimal`
  - `InsufficientInventoryException extends DomainException` → 422
  - `GET /api/v1/assets/{symbol}/inventory` (public), `PUT /api/v1/assets/{symbol}/inventory` (ADMIN)

> **Lock ordering — read this before Task 10.** The buy path takes two row locks: the wallet and the inventory. Every code path must acquire them in the same order — **wallet first, then inventory** — or two concurrent trades on opposite assets will deadlock. `TransactionService` is the only place that holds both.

- [ ] **Step 1: Write the failing `AssetInventoryIT`**

```java
package com.criptoativos.asset.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.criptoativos.support.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class AssetInventoryIT extends AbstractIT {

    @Autowired MockMvc mockMvc;

    @Test
    void seededAssetsExposeAnInventory() throws Exception {
        mockMvc.perform(get("/api/v1/assets/BTC/inventory"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol").value("BTC"))
            .andExpect(jsonPath("$.availableQuantity").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdminCanRestock() throws Exception {
        mockMvc.perform(put("/api/v1/assets/BTC/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availableQuantity\":\"250.00000000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.availableQuantity").value("250.00000000"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void aPlainUserCannotRestock() throws Exception {
        mockMvc.perform(put("/api/v1/assets/BTC/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availableQuantity\":\"250\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void restockingRejectsANegativeQuantity() throws Exception {
        mockMvc.perform(put("/api/v1/assets/BTC/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"availableQuantity\":\"-1\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSymbolReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/assets/NOPE/inventory"))
            .andExpect(status().isNotFound());
    }
}
```

Plus a unit test `AssetInventoryTest` asserting: `reserve` beyond `availableQuantity` throws `InsufficientInventoryException` and leaves the quantity untouched; `reserve` then `release` of the same amount is a round trip; `reserve(0)` and `reserve(-1)` are rejected.

- [ ] **Step 2: Run it, confirm it fails**

- [ ] **Step 3: Implement `InsufficientInventoryException`**

```java
package com.criptoativos.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientInventoryException extends DomainException {
    public InsufficientInventoryException(String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient inventory", detail);
    }
}
```

- [ ] **Step 4: Implement `AssetInventory`**

Shares its primary key with `assets` via `@MapsId`, so there is exactly one inventory row per asset and no separate id to keep in sync.

```java
package com.criptoativos.asset.inventory;

import com.criptoativos.asset.Asset;
import com.criptoativos.common.Money;
import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.common.exception.InsufficientInventoryException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "asset_inventory")
public class AssetInventory {

    @Id
    @Column(name = "asset_id")
    private UUID assetId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(name = "available_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal availableQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version private long version;

    protected AssetInventory() {}

    public static AssetInventory forAsset(Asset asset, BigDecimal initialQuantity) {
        AssetInventory inventory = new AssetInventory();
        inventory.asset = asset;
        inventory.availableQuantity = Money.units(initialQuantity);
        return inventory;
    }

    @PrePersist @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /** Removes quantity from the exchange's available supply, for a buy. */
    public void reserve(BigDecimal quantity) {
        requirePositive(quantity);
        if (availableQuantity.compareTo(quantity) < 0) {
            throw new InsufficientInventoryException(
                "Only %s %s available, requested %s."
                    .formatted(availableQuantity, asset.getSymbol(), Money.units(quantity)));
        }
        this.availableQuantity = Money.units(availableQuantity.subtract(quantity));
    }

    /** Returns quantity to the exchange's available supply, for a sell. */
    public void release(BigDecimal quantity) {
        requirePositive(quantity);
        this.availableQuantity = Money.units(availableQuantity.add(quantity));
    }

    public void restock(BigDecimal newQuantity) {
        if (newQuantity == null || newQuantity.signum() < 0) {
            throw new BusinessRuleException("Available quantity cannot be negative.");
        }
        this.availableQuantity = Money.units(newQuantity);
    }

    private static void requirePositive(BigDecimal value) {
        if (!Money.isPositive(value)) {
            throw new BusinessRuleException("Quantity must be greater than zero.");
        }
    }

    public UUID getAssetId() { return assetId; }
    public Asset getAsset() { return asset; }
    public BigDecimal getAvailableQuantity() { return availableQuantity; }
    public Instant getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 5: Implement the repository with a pessimistic lock**

```java
public interface AssetInventoryRepository extends JpaRepository<AssetInventory, UUID> {

    /** Row-level lock. Callers holding a wallet lock must already hold it — see the lock-ordering note. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from AssetInventory i where i.assetId = :assetId")
    Optional<AssetInventory> findByAssetIdForUpdate(@Param("assetId") UUID assetId);
}
```

- [ ] **Step 6: Implement `AssetInventoryService`, DTOs, and the controller**

`reserve` / `release` load with `findByAssetIdForUpdate` and delegate to the entity. `restock(symbol, qty)` resolves the asset via `AssetService.requireBySymbol` first, so an unknown symbol yields 404 rather than a confusing inventory error.

```java
public record RestockRequest(
    @NotNull @DecimalMin(value = "0", message = "must not be negative") BigDecimal availableQuantity) {}

public record InventoryResponse(String symbol, BigDecimal availableQuantity, Instant updatedAt) {
    public static InventoryResponse from(AssetInventory inventory) {
        return new InventoryResponse(
            inventory.getAsset().getSymbol(), inventory.getAvailableQuantity(), inventory.getUpdatedAt());
    }
}
```

Controller lives at `/api/v1/assets/{symbol}/inventory`: `GET` is public, `PUT` carries `@PreAuthorize("hasRole('ADMIN')")`.

- [ ] **Step 7: Create inventory alongside every new asset**

In `AssetService.createCrypto`, after saving the asset, persist `AssetInventory.forAsset(asset, BigDecimal.ZERO)`. Add an `AssetIT` assertion that a newly created asset immediately exposes an inventory of `0.00000000` — an asset with no inventory row would blow up the first buy with a `NotFoundException`.

- [ ] **Step 8: Seed inventory for the existing assets**

Append to `V2__seed_assets.sql`:
```sql
insert into asset_inventory (asset_id, available_quantity)
select id, 1000 from assets
on conflict (asset_id) do nothing;
```

- [ ] **Step 9: Expose availability on `AssetResponse`**

Add `BigDecimal availableQuantity` so `GET /api/v1/assets` answers "can I buy this?" in one call. Populate it in `AssetService.list` with a single join query rather than N+1 lookups:

```java
@Query("select a, i.availableQuantity from Asset a left join AssetInventory i on i.assetId = a.id")
Page<Object[]> findAllWithInventory(Pageable pageable);
```

- [ ] **Step 10: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add exchange-side asset inventory with admin restocking"
```

---

## Task 9: Wallet — balance, deposits, withdrawals

**Files:**
- Modify: `wallet/Wallet.java` (the stub from Task 4 gains behaviour)
- Create: `wallet/{Holding,WalletRepository,WalletService,WalletController}.java`
- Create: `operation/{Operation,CashOperation,CashOperationType,OperationRepository}.java`
- Create: `wallet/dto/{CashAmountRequest,WalletResponse,HoldingResponse}.java`
- Create: `src/test/java/com/criptoativos/wallet/WalletServiceTest.java`, `.../wallet/WalletIT.java`

**Interfaces:**
- Consumes: `User` (Task 4), `Asset` (Task 7), `Money`, `BusinessRuleException`, `NotFoundException` (Task 3), `SecurityUtils.currentUserId()` (Task 5).
- Produces:
  - `Wallet` with `getUser():User`, `getCashBalance():BigDecimal`, `getHoldings():Set<Holding>`, `credit(BigDecimal)`, `debit(BigDecimal)`, `addHolding(Asset, BigDecimal qty, BigDecimal unitPrice)`, `reduceHolding(Asset, BigDecimal qty)`, `findHolding(Asset):Optional<Holding>`
  - `Holding` with `getAsset():Asset`, `getQuantity():BigDecimal`, `getAverageCost():BigDecimal`
  - `Operation` (abstract, `@Inheritance(SINGLE_TABLE)`, discriminator `operation_type`); `CashOperation` (`DEPOSIT`/`WITHDRAWAL`)
  - `WalletRepository.findByUserIdForUpdate(UUID) : Optional<Wallet>` — `@Lock(PESSIMISTIC_WRITE)`, consumed by Task 8
  - `WalletService.deposit(UUID userId, BigDecimal amount) : Wallet`, `.withdraw(...)`, `.requireByUserId(UUID) : Wallet`
  - `GET /api/v1/wallet`, `POST /api/v1/wallet/deposits`, `POST /api/v1/wallet/withdrawals`

- [ ] **Step 1: Write the failing `WalletServiceTest`**

```java
package com.criptoativos.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.criptoativos.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    void creditIncreasesTheBalanceAtScaleTwo() {
        Wallet wallet = Wallet.forUser(null);
        wallet.credit(new BigDecimal("100.005"));
        assertThat(wallet.getCashBalance()).hasToString("100.00");
    }

    @Test
    void debitRejectsAnAmountLargerThanTheBalance() {
        Wallet wallet = Wallet.forUser(null);
        wallet.credit(new BigDecimal("50.00"));

        assertThatThrownBy(() -> wallet.debit(new BigDecimal("50.01")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient funds");

        assertThat(wallet.getCashBalance()).hasToString("50.00"); // unchanged
    }

    @Test
    void creditRejectsANonPositiveAmount() {
        Wallet wallet = Wallet.forUser(null);
        assertThatThrownBy(() -> wallet.credit(BigDecimal.ZERO))
            .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> wallet.credit(new BigDecimal("-1")))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void addHoldingComputesAWeightedAverageCost() {
        Wallet wallet = Wallet.forUser(null);
        Asset btc = TestAssets.btc();

        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("100"));
        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("200"));

        Holding holding = wallet.findHolding(btc).orElseThrow();
        assertThat(holding.getQuantity()).isEqualByComparingTo("2");
        assertThat(holding.getAverageCost()).isEqualByComparingTo("150");
    }

    @Test
    void addHoldingDoesNotDuplicateTheSameAsset() {
        Wallet wallet = Wallet.forUser(null);
        Asset btc = TestAssets.btc();
        wallet.addHolding(btc, BigDecimal.ONE, new BigDecimal("100"));
        wallet.addHolding(btc, BigDecimal.ONE, new BigDecimal("100"));

        assertThat(wallet.getHoldings()).hasSize(1); // the original HashMap bug, now impossible
    }

    @Test
    void reduceHoldingRemovesTheHoldingWhenFullySold() {
        Wallet wallet = Wallet.forUser(null);
        Asset btc = TestAssets.btc();
        wallet.addHolding(btc, new BigDecimal("2"), new BigDecimal("100"));
        wallet.reduceHolding(btc, new BigDecimal("2"));

        assertThat(wallet.getHoldings()).isEmpty();
    }

    @Test
    void reduceHoldingRejectsSellingMoreThanIsHeld() {
        Wallet wallet = Wallet.forUser(null);
        Asset btc = TestAssets.btc();
        wallet.addHolding(btc, new BigDecimal("1"), new BigDecimal("100"));

        assertThatThrownBy(() -> wallet.reduceHolding(btc, new BigDecimal("1.5")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient holdings");
    }
}
```

`TestAssets` lives in `src/test/java/com/criptoativos/support/TestAssets.java` and returns `CryptoAsset.create("BTC", "Bitcoin", new BigDecimal("100"), "bitcoin")`.

- [ ] **Step 2: Run it, confirm failure**

- [ ] **Step 3: Implement `Holding`**

Entity mapping `holdings`, fields `wallet`, `asset`, `quantity`, `averageCost`, `@Version`. Package-private mutators `increase(BigDecimal qty, BigDecimal unitPrice)` (recomputes weighted average cost) and `decrease(BigDecimal qty)`. Uniqueness is enforced by the DB constraint `uq_holdings_wallet_asset` **and** by `Wallet` looking the holding up before inserting.

Weighted average:
```java
void increase(BigDecimal addedQuantity, BigDecimal unitPrice) {
    BigDecimal existingCost = this.quantity.multiply(this.averageCost);
    BigDecimal addedCost = addedQuantity.multiply(unitPrice);
    BigDecimal newQuantity = this.quantity.add(addedQuantity);
    this.averageCost = Money.units(existingCost.add(addedCost).divide(newQuantity, Money.UNIT_SCALE, Money.ROUNDING));
    this.quantity = Money.units(newQuantity);
}
```

- [ ] **Step 4: Implement `Wallet`**

Key methods — all invariants live here, not in the service:
```java
public void credit(BigDecimal amount) {
    requirePositive(amount);
    this.cashBalance = Money.cash(this.cashBalance.add(amount));
}

public void debit(BigDecimal amount) {
    requirePositive(amount);
    BigDecimal rounded = Money.cash(amount);
    if (this.cashBalance.compareTo(rounded) < 0) {
        throw new BusinessRuleException(
            "Insufficient funds: balance is %s, required %s.".formatted(this.cashBalance, rounded));
    }
    this.cashBalance = Money.cash(this.cashBalance.subtract(rounded));
}

public Optional<Holding> findHolding(Asset asset) {
    return holdings.stream().filter(h -> h.getAsset().getSymbol().equals(asset.getSymbol())).findFirst();
}

public void addHolding(Asset asset, BigDecimal quantity, BigDecimal unitPrice) {
    requirePositive(quantity);
    findHolding(asset).ifPresentOrElse(
        holding -> holding.increase(quantity, unitPrice),
        () -> holdings.add(Holding.open(this, asset, quantity, unitPrice)));
}

public void reduceHolding(Asset asset, BigDecimal quantity) {
    requirePositive(quantity);
    Holding holding = findHolding(asset).orElseThrow(() ->
        new BusinessRuleException("Insufficient holdings: you do not own %s.".formatted(asset.getSymbol())));
    if (holding.getQuantity().compareTo(quantity) < 0) {
        throw new BusinessRuleException("Insufficient holdings: you own %s %s, tried to sell %s."
            .formatted(holding.getQuantity(), asset.getSymbol(), quantity));
    }
    holding.decrease(quantity);
    if (holding.getQuantity().signum() == 0) {
        holdings.remove(holding);   // orphanRemoval deletes the row
    }
}

private static void requirePositive(BigDecimal value) {
    if (!Money.isPositive(value)) {
        throw new BusinessRuleException("Amount must be greater than zero.");
    }
}
```

`holdings` is `@OneToMany(mappedBy="wallet", cascade=ALL, orphanRemoval=true)` on a `LinkedHashSet`.

- [ ] **Step 5: Implement `Operation` and `CashOperation`**

`Operation` is abstract with `id`, `user`, `description`, `occurredAt`, `@PrePersist` setting `occurredAt`, and an abstract `String summary()` — the honest replacement for the original `executarOperacao()` that only printed to stdout.

`CashOperation` (`@DiscriminatorValue("CASH")`) adds `cashType` (`CashOperationType.DEPOSIT|WITHDRAWAL`) and `amount`, with `static CashOperation deposit(User, BigDecimal)` / `withdrawal(User, BigDecimal)`.

This is what justifies keeping the `Operation` hierarchy: it now has two real subclasses, not one.

- [ ] **Step 6: Implement `WalletRepository` with a pessimistic lock**

```java
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUserId(UUID userId);

    /** Row-level lock so concurrent buys on the same wallet serialise instead of racing. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") UUID userId);
}
```

- [ ] **Step 7: Implement `WalletService`**

`deposit` and `withdraw` are `@Transactional`, load the wallet with `findByUserIdForUpdate`, call `credit`/`debit`, and persist a `CashOperation` through `OperationRepository`. `getWallet` is `@Transactional(readOnly = true)` and uses `findByUserId`.

- [ ] **Step 8: Implement DTOs and `WalletController`**

`CashAmountRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount)`.
`WalletResponse(UUID id, BigDecimal cashBalance, List<HoldingResponse> holdings)` — portfolio valuation fields arrive in Task 12.
Endpoints resolve the user with `SecurityUtils.currentUserId()`; there is no `userId` path variable, so one user can never read another's wallet.

- [ ] **Step 9: Write `WalletIT`**

Covers: `GET /api/v1/wallet` on a fresh account returns `0.00` and no holdings; deposit then read back; withdrawal beyond balance → 422 with `title: "Business rule violated"`; deposit of `0` → 400 validation error; `GET /api/v1/wallet` without a token → 401.

- [ ] **Step 10: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add wallet with cash deposits, withdrawals, and holdings"
```

---

## Task 10: Buy and sell transactions

This is where every original `Carteira`/`Main` bug gets fixed at once.

**Files:**
- Create: `operation/{Transaction,TransactionType,TransactionService,TransactionController}.java`
- Create: `operation/dto/{TradeRequest,TransactionResponse}.java`
- Modify: `operation/OperationRepository.java` (add paged history query)
- Create: `src/test/java/com/criptoativos/operation/TransactionServiceIT.java`, `.../operation/TradingIT.java`

**Interfaces:**
- Consumes: `Wallet.debit/credit/addHolding/reduceHolding`, `WalletRepository.findByUserIdForUpdate` (Task 9); `AssetService.requireBySymbol` (Task 7); `AssetInventoryService.reserve/release` (Task 8).
- Produces:
  - `TransactionType` enum `{ BUY, SELL }`
  - `Transaction extends Operation` (`@DiscriminatorValue("TRANSACTION")`) with `getAsset():Asset`, `getTransactionType():TransactionType`, `getQuantity():BigDecimal`, `getUnitPrice():BigDecimal`, `getTotalAmount():BigDecimal`
  - `TransactionService.buy(UUID userId, String symbol, BigDecimal quantity) : Transaction`, `.sell(...)`, `.history(UUID userId, TransactionFilter, Pageable) : Page<Transaction>`
  - `POST /api/v1/transactions/buy`, `POST /api/v1/transactions/sell`, `GET /api/v1/transactions`

- [ ] **Step 1: Write the failing `TransactionServiceIT`**

The critical tests — each one pins a bug from the original code:

```java
package com.criptoativos.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.criptoativos.common.exception.BusinessRuleException;
import com.criptoativos.support.AbstractIT;
import com.criptoativos.wallet.Wallet;
import com.criptoativos.wallet.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TransactionServiceIT extends AbstractIT {

    @Autowired TransactionService transactionService;
    @Autowired WalletService walletService;
    @Autowired com.criptoativos.asset.AssetService assetService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = TestFixtures.registerUserWithBalance(new BigDecimal("10000.00"));
        assetService.updatePrice("BTC", new BigDecimal("1000.00"));
    }

    @Test
    void buyDebitsCashAndCreatesAHolding() {
        transactionService.buy(userId, "BTC", new BigDecimal("2"));

        Wallet wallet = walletService.requireByUserId(userId);
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("8000.00");
        assertThat(wallet.getHoldings()).hasSize(1);
        assertThat(wallet.getHoldings().iterator().next().getQuantity()).isEqualByComparingTo("2");
    }

    /** Original bug: Main.java:101-103 recorded the asset and printed success even when the buy failed. */
    @Test
    void buyWithInsufficientFundsChangesNothingAtAll() {
        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", new BigDecimal("11")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient funds");

        Wallet wallet = walletService.requireByUserId(userId);
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("10000.00");
        assertThat(wallet.getHoldings()).isEmpty();
        assertThat(transactionService.history(userId, null, org.springframework.data.domain.Pageable.unpaged()))
            .isEmpty();
    }

    /** Original bug: Carteira.java:64 sold at the price captured when the asset object was built. */
    @Test
    void sellUsesTheCurrentPriceNotThePurchasePrice() {
        transactionService.buy(userId, "BTC", new BigDecimal("2"));   // at 1000
        assetService.updatePrice("BTC", new BigDecimal("1500.00"));
        transactionService.sell(userId, "BTC", new BigDecimal("2"));  // must settle at 1500

        assertThat(walletService.requireByUserId(userId).getCashBalance()).isEqualByComparingTo("11000.00");
    }

    @Test
    void sellingEverythingRemovesTheHolding() {
        transactionService.buy(userId, "BTC", new BigDecimal("1"));
        transactionService.sell(userId, "BTC", new BigDecimal("1"));

        assertThat(walletService.requireByUserId(userId).getHoldings()).isEmpty();
    }

    @Test
    void sellingMoreThanIsHeldIsRejected() {
        transactionService.buy(userId, "BTC", new BigDecimal("1"));

        assertThatThrownBy(() -> transactionService.sell(userId, "BTC", new BigDecimal("2")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Insufficient holdings");
    }

    @Test
    void buyingTheSameAssetTwiceMergesIntoOneHolding() {
        transactionService.buy(userId, "BTC", new BigDecimal("1"));
        transactionService.buy(userId, "BTC", new BigDecimal("1"));

        assertThat(walletService.requireByUserId(userId).getHoldings()).hasSize(1);
    }

    @Test
    void aNonPositiveQuantityIsRejected() {
        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", BigDecimal.ZERO))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void buyingDrawsDownTheExchangeInventory() {
        BigDecimal before = inventoryService.available("BTC");
        transactionService.buy(userId, "BTC", new BigDecimal("3"));

        assertThat(inventoryService.available("BTC")).isEqualByComparingTo(before.subtract(new BigDecimal("3")));
    }

    @Test
    void sellingReturnsQuantityToTheExchangeInventory() {
        BigDecimal before = inventoryService.available("BTC");
        transactionService.buy(userId, "BTC", new BigDecimal("3"));
        transactionService.sell(userId, "BTC", new BigDecimal("3"));

        assertThat(inventoryService.available("BTC")).isEqualByComparingTo(before);
    }

    @Test
    void buyingMoreThanTheExchangeHoldsIsRejectedAndRefundsNothing() {
        inventoryService.restock("BTC", new BigDecimal("2"));

        assertThatThrownBy(() -> transactionService.buy(userId, "BTC", new BigDecimal("5")))
            .isInstanceOf(InsufficientInventoryException.class);

        Wallet wallet = walletService.requireByUserId(userId);
        assertThat(wallet.getCashBalance()).isEqualByComparingTo("10000.00");   // debit rolled back
        assertThat(wallet.getHoldings()).isEmpty();
        assertThat(inventoryService.available("BTC")).isEqualByComparingTo("2");
    }
}
```

The last test is the one that proves `@Transactional` is doing real work: the wallet is debited *before* the inventory check throws, so only a rollback can leave the balance at `10000.00`.

- [ ] **Step 2: Write the concurrency test**

The single most resume-worthy test in the project — it proves the pessimistic lock does its job.

```java
@Test
void concurrentBuysCanNeverOverdrawTheWallet() throws Exception {
    assetService.updatePrice("BTC", new BigDecimal("6000.00")); // balance 10000 affords exactly one
    int threads = 8;
    var barrier = new java.util.concurrent.CyclicBarrier(threads);
    var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
    var succeeded = new java.util.concurrent.atomic.AtomicInteger();

    var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
    for (int i = 0; i < threads; i++) {
        futures.add(executor.submit(() -> {
            barrier.await();
            try {
                transactionService.buy(userId, "BTC", BigDecimal.ONE);
                succeeded.incrementAndGet();
            } catch (BusinessRuleException expected) {
                // losing threads are supposed to be rejected
            }
            return null;
        }));
    }
    for (var future : futures) {
        future.get(30, java.util.concurrent.TimeUnit.SECONDS);
    }
    executor.shutdown();

    assertThat(succeeded.get()).isEqualTo(1);
    assertThat(walletService.requireByUserId(userId).getCashBalance()).isEqualByComparingTo("4000.00");
}
```

> This test must **not** be `@Transactional` — it needs real committed transactions across threads. Annotate the class or method accordingly and clean up explicitly.

- [ ] **Step 3: Run the tests, confirm they fail**

- [ ] **Step 4: Implement `TransactionType` and `Transaction`**

```java
package com.criptoativos.operation;

public enum TransactionType {
    BUY,
    SELL
}
```

`Transaction` stores the executed `unitPrice` and `totalAmount` — a permanent, immutable record of what actually happened, rather than recomputing from today's price:
```java
public static Transaction of(User user, Asset asset, TransactionType type,
                             BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalAmount) {
    Transaction tx = new Transaction();
    tx.initialise(user, "%s %s %s @ %s".formatted(type, quantity, asset.getSymbol(), unitPrice));
    tx.asset = asset;
    tx.transactionType = type;
    tx.quantity = Money.units(quantity);
    tx.unitPrice = Money.units(unitPrice);
    tx.totalAmount = Money.cash(totalAmount);
    return tx;
}
```

- [ ] **Step 5: Implement `TransactionService`**

```java
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final WalletRepository walletRepository;
    private final AssetService assetService;
    private final OperationRepository operationRepository;

    // constructor omitted for brevity — standard constructor injection

    // Lock order is always wallet -> inventory. Never reverse it; see the Task 8 lock-ordering note.

    @Transactional
    public Transaction buy(UUID userId, String symbol, BigDecimal quantity) {
        Asset asset = assetService.requireBySymbol(symbol);
        Wallet wallet = requireLockedWallet(userId);             // lock 1

        BigDecimal unitPrice = asset.getCurrentPrice();          // snapshot at execution time
        BigDecimal total = Money.cash(unitPrice.multiply(quantity));

        wallet.debit(total);                                     // throws before anything is recorded
        inventoryService.reserve(asset, quantity);               // lock 2; throws if supply is short
        wallet.addHolding(asset, quantity, unitPrice);

        Transaction tx = Transaction.of(
            wallet.getUser(), asset, TransactionType.BUY, quantity, unitPrice, total);
        Transaction saved = operationRepository.save(tx);
        log.info("BUY user={} asset={} qty={} unitPrice={} total={}", userId, symbol, quantity, unitPrice, total);
        return saved;
    }

    @Transactional
    public Transaction sell(UUID userId, String symbol, BigDecimal quantity) {
        Asset asset = assetService.requireBySymbol(symbol);
        Wallet wallet = requireLockedWallet(userId);             // lock 1

        BigDecimal unitPrice = asset.getCurrentPrice();
        BigDecimal proceeds = Money.cash(unitPrice.multiply(quantity));

        wallet.reduceHolding(asset, quantity);                   // throws before cash moves
        inventoryService.release(asset, quantity);               // lock 2; supply returns to the exchange
        wallet.credit(proceeds);

        Transaction tx = Transaction.of(
            wallet.getUser(), asset, TransactionType.SELL, quantity, unitPrice, proceeds);
        Transaction saved = operationRepository.save(tx);
        log.info("SELL user={} asset={} qty={} unitPrice={} total={}", userId, symbol, quantity, unitPrice, proceeds);
        return saved;
    }

    private Wallet requireLockedWallet(UUID userId) {
        return walletRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new NotFoundException("Wallet for user %s does not exist.".formatted(userId)));
    }
}
```

`@Transactional` means an exception anywhere rolls the whole thing back — the atomicity the original console app never had.

- [ ] **Step 6: Implement the history query**

```java
@Query("""
    select t from Transaction t
    where t.user.id = :userId
      and (:type is null or t.transactionType = :type)
      and (:symbol is null or upper(t.asset.symbol) = upper(:symbol))
      and (:from is null or t.occurredAt >= :from)
      and (:to   is null or t.occurredAt <= :to)
    """)
Page<Transaction> findHistory(@Param("userId") UUID userId,
                              @Param("type") TransactionType type,
                              @Param("symbol") String symbol,
                              @Param("from") Instant from,
                              @Param("to") Instant to,
                              Pageable pageable);
```

- [ ] **Step 7: Implement `TradeRequest`, `TransactionResponse`, and `TransactionController`**

```java
public record TradeRequest(
    @NotBlank @Size(max = 20) String symbol,
    @NotNull @DecimalMin(value = "0.00000001") BigDecimal quantity) {}
```

Controller exposes `POST /buy`, `POST /sell` (both `201 Created`) and `GET /` with optional `type`, `symbol`, `from`, `to` query params plus `@PageableDefault(size = 20, sort = "occurredAt", direction = DESC)`.

- [ ] **Step 8: Write `TradingIT`** — end-to-end over HTTP: register → login → deposit → buy → history shows the buy → sell → wallet reflects it.

- [ ] **Step 9: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add atomic buy/sell trading with pessimistic wallet locking"
```

---

## Task 11: Live prices from CoinGecko

**Files:**
- Create: `asset/price/{PriceProvider,PriceQuote,CoinGeckoPriceProvider,ManualPriceProvider,PriceRefreshJob}.java`
- Create: `config/{CacheConfig,SchedulingConfig,RestClientConfig}.java`
- Modify: `application.yml`, `AssetService.java`
- Create: `src/test/java/com/criptoativos/asset/price/{CoinGeckoPriceProviderTest,PriceRefreshJobTest}.java`

**Interfaces:**
- Consumes: `CryptoAsset.updatePrice(BigDecimal, BigDecimal)`, `AssetRepository.findAllByExternalIdNotNull()` (Task 7).
- Produces:
  - `PriceQuote(String externalId, BigDecimal price, BigDecimal dailyChangePct)`
  - `PriceProvider.fetchQuotes(Collection<String> externalIds) : Map<String, PriceQuote>`
  - `PriceRefreshJob.refresh()` — `@Scheduled`, and callable manually
  - `POST /api/v1/assets/refresh-prices` (ADMIN) to trigger a refresh on demand

- [ ] **Step 1: Add configuration**

```yaml
app:
  prices:
    provider: ${PRICE_PROVIDER:coingecko}      # coingecko | manual
    refresh-cron: "0 */5 * * * *"              # every 5 minutes
    coingecko:
      base-url: https://api.coingecko.com/api/v3
      vs-currency: usd
      timeout: PT5S
```

- [ ] **Step 2: Define the port**

```java
package com.criptoativos.asset.price;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/** Port for external market data. The domain never speaks HTTP. */
public interface PriceProvider {

    record PriceQuote(String externalId, BigDecimal price, BigDecimal dailyChangePct) {}

    /** @return quotes keyed by external id; ids the provider does not know are simply absent. */
    Map<String, PriceQuote> fetchQuotes(Collection<String> externalIds);
}
```

- [ ] **Step 3: Write the failing `CoinGeckoPriceProviderTest`** using `MockRestServiceServer`

```java
package com.criptoativos.asset.price;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CoinGeckoPriceProviderTest {

    private MockRestServiceServer server;
    private CoinGeckoPriceProvider provider;

    private static final String BODY = """
        {"bitcoin":{"usd":64250.12,"usd_24h_change":2.4517},
         "ethereum":{"usd":3120.55,"usd_24h_change":-1.2033}}
        """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new CoinGeckoPriceProvider(builder.build(), "usd");
    }

    @Test
    void mapsTheResponseIntoQuotes() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("ids=bitcoin,ethereum")))
            .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        var quotes = provider.fetchQuotes(List.of("bitcoin", "ethereum"));

        assertThat(quotes).hasSize(2);
        assertThat(quotes.get("bitcoin").price()).isEqualByComparingTo("64250.12");
        assertThat(quotes.get("ethereum").dailyChangePct()).isEqualByComparingTo("-1.2033");
    }

    @Test
    void returnsAnEmptyMapWhenTheProviderIsDown() {
        server.expect(requestTo(org.hamcrest.Matchers.anything())).andRespond(withServerError());

        assertThat(provider.fetchQuotes(List.of("bitcoin"))).isEmpty();
    }

    @Test
    void skipsTheCallEntirelyForAnEmptyIdList() {
        assertThat(provider.fetchQuotes(List.of())).isEmpty();
        server.verify(); // no request was made
    }
}
```

The second test is the important one: an outage must degrade to "prices are stale", never to a 500 on every endpoint.

- [ ] **Step 4: Implement `CoinGeckoPriceProvider`**

Calls `/simple/price?ids={ids}&vs_currencies={cur}&include_24hr_change=true`, deserializes into `Map<String, Map<String, BigDecimal>>`, catches `RestClientException`, logs at WARN, and returns `Map.of()`. Annotate the class `@Component @ConditionalOnProperty(name = "app.prices.provider", havingValue = "coingecko", matchIfMissing = true)`.

`ManualPriceProvider` (`havingValue = "manual"`) returns `Map.of()` and exists so the app boots with no network — used by every integration test via `application-test.yml`.

- [ ] **Step 5: Implement `PriceRefreshJob`**

```java
@Component
public class PriceRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(PriceRefreshJob.class);

    private final AssetRepository assetRepository;
    private final PriceProvider priceProvider;

    @Scheduled(cron = "${app.prices.refresh-cron}")
    @Transactional
    public void refresh() {
        List<CryptoAsset> tracked = assetRepository.findAllByExternalIdNotNull();
        if (tracked.isEmpty()) {
            return;
        }
        Map<String, PriceProvider.PriceQuote> quotes = priceProvider.fetchQuotes(
            tracked.stream().map(CryptoAsset::getExternalId).toList());

        int updated = 0;
        for (CryptoAsset asset : tracked) {
            PriceProvider.PriceQuote quote = quotes.get(asset.getExternalId());
            if (quote != null) {
                asset.updatePrice(quote.price(), quote.dailyChangePct());
                updated++;
            }
        }
        log.info("Refreshed {} of {} tracked asset prices", updated, tracked.size());
    }
}
```

Enable with `@EnableScheduling` in `SchedulingConfig`, guarded by `@Profile("!test")` so the scheduler never fires during tests.

- [ ] **Step 6: Add `PriceRefreshJobTest`** with a stub `PriceProvider` — assert that an asset whose id is missing from the response keeps its previous price and that the job does not throw.

- [ ] **Step 7: Add the admin refresh endpoint and run the app against the real API**

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
# in another shell:
curl http://localhost:8080/api/v1/assets
```
Expected: within one cron tick, `BTC` has a non-zero `currentPrice` and a populated `priceUpdatedAt`.

- [ ] **Step 8: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: fetch live prices from CoinGecko behind a PriceProvider port"
```

---

## Task 12: Portfolio valuation and P&L

**Files:**
- Create: `wallet/PortfolioService.java`
- Modify: `wallet/dto/{WalletResponse,HoldingResponse}.java`, `wallet/WalletController.java`
- Create: `src/test/java/com/criptoativos/wallet/PortfolioServiceTest.java`

**Interfaces:**
- Consumes: `Wallet.getHoldings()`, `Holding.getAverageCost()` (Task 9); `Asset.getCurrentPrice()` (Task 7).
- Produces: `PortfolioService.summarise(Wallet) : PortfolioSummary` where
  `PortfolioSummary(BigDecimal cashBalance, BigDecimal investedValue, BigDecimal marketValue, BigDecimal totalValue, BigDecimal unrealisedPnl, BigDecimal unrealisedPnlPct, List<HoldingResponse> holdings)`
  and `HoldingResponse(String symbol, String name, BigDecimal quantity, BigDecimal averageCost, BigDecimal currentPrice, BigDecimal marketValue, BigDecimal unrealisedPnl, BigDecimal unrealisedPnlPct)`.

- [ ] **Step 1: Write the failing `PortfolioServiceTest`**

Pure computation, no Spring. Assert: 2 BTC bought at 100, now worth 150 → `marketValue` 300.00, `investedValue` 200.00, `unrealisedPnl` 100.00, `unrealisedPnlPct` 50.0000; an empty wallet yields all zeros and **no division by zero**; `totalValue` = cash + marketValue.

- [ ] **Step 2: Run, confirm failure, implement**

Guard the percentage explicitly:
```java
BigDecimal pct = invested.signum() == 0
    ? BigDecimal.ZERO.setScale(4)
    : pnl.multiply(HUNDRED).divide(invested, 4, Money.ROUNDING);
```

- [ ] **Step 3: Wire it into `GET /api/v1/wallet`, extend `WalletIT` to assert the P&L fields, run `./mvnw verify`, commit**

```powershell
git add -A
git commit -m "feat: add portfolio valuation with unrealised profit and loss"
```

---

## Task 13: Admin user management

The remote version's menu had "list users" and "delete user" available to anyone at the console. Here they become genuinely admin-only endpoints with the guardrails the original lacked.

**Files:**
- Create: `admin/{AdminUserController,AdminUserService,AdminSeeder}.java`
- Create: `admin/dto/{AdminUserResponse,ChangeRoleRequest}.java`
- Modify: `user/UserRepository.java` (search query), `application.yml`
- Create: `src/test/java/com/criptoativos/admin/AdminUserIT.java`

**Interfaces:**
- Consumes: `User`, `Role`, `UserRepository`, `UserService` (Task 4); `SecurityUtils.currentUserId()`, `ROLE_ADMIN` (Task 5); `Wallet.getCashBalance/getHoldings` (Task 9).
- Produces:
  - `AdminUserService.list(String search, Pageable) : Page<User>`, `.get(UUID) : User`, `.delete(UUID actorId, UUID targetId, boolean force)`, `.changeRole(UUID actorId, UUID targetId, Role) : User`
  - `AdminUserResponse(UUID id, String name, String email, String cpfMasked, String role, boolean twoFactorEnabled, BigDecimal cashBalance, int holdingCount, Instant createdAt)`
  - `GET /api/v1/admin/users`, `GET /api/v1/admin/users/{id}`, `DELETE /api/v1/admin/users/{id}`, `PATCH /api/v1/admin/users/{id}/role` — all `hasRole('ADMIN')`

- [ ] **Step 1: Write the failing `AdminUserIT`**

The guardrail tests matter more than the happy paths:

```java
package com.criptoativos.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.criptoativos.support.AbstractIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class AdminUserIT extends AbstractIT {

    @Autowired MockMvc mockMvc;

    @Test
    void listingUsersRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + TestFixtures.userToken()))
            .andExpect(status().isForbidden());
    }

    @Test
    void anAdminSeesAPagedUserListWithMaskedCpf() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].email").isNotEmpty())
            .andExpect(jsonPath("$.content[0].cpfMasked").value(org.hamcrest.Matchers.matchesPattern("\\*{3}\\.\\*{3}\\.\\d{3}-\\d{2}")))
            .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
            .andExpect(jsonPath("$.content[0].twoFactorSecret").doesNotExist());
    }

    @Test
    void searchFiltersByEmailFragment() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").param("search", "ana")
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].email").value(org.hamcrest.Matchers.everyItem(
                org.hamcrest.Matchers.containsString("ana"))));
    }

    @Test
    void anAdminCannotDeleteThemselves() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/" + TestFixtures.adminId())
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value("You cannot delete your own account."));
    }

    @Test
    void deletingAUserWithFundsIsRefusedWithoutForce() throws Exception {
        var funded = TestFixtures.registerUserWithBalance(new java.math.BigDecimal("500.00"));

        mockMvc.perform(delete("/api/v1/admin/users/" + funded)
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("non-zero balance")));

        mockMvc.perform(delete("/api/v1/admin/users/" + funded).param("force", "true")
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isNoContent());
    }

    @Test
    void theLastAdminCannotBeDemoted() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + TestFixtures.adminId() + "/role")
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}")
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.detail").value("The last administrator cannot be demoted."));
    }

    @Test
    void deletingAUserCascadesToTheirWalletAndOperations() throws Exception {
        var target = TestFixtures.registerUserWithBalance(java.math.BigDecimal.ZERO);

        mockMvc.perform(delete("/api/v1/admin/users/" + target)
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/users/" + target)
                .header("Authorization", "Bearer " + TestFixtures.adminToken()))
            .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: Run it, confirm it fails**

- [ ] **Step 3: Add the search query to `UserRepository`**

```java
@Query("""
    select u from User u
    where :search is null
       or lower(u.email) like lower(concat('%', :search, '%'))
       or lower(u.name)  like lower(concat('%', :search, '%'))
    """)
Page<User> search(@Param("search") String search, Pageable pageable);

long countByRole(Role role);
```

- [ ] **Step 4: Implement `AdminUserService`**

```java
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Page<User> list(String search, Pageable pageable) {
        return userRepository.search(search, pageable);
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User %s does not exist.".formatted(id)));
    }

    @Transactional
    public void delete(UUID actorId, UUID targetId, boolean force) {
        if (actorId.equals(targetId)) {
            throw new BusinessRuleException("You cannot delete your own account.");
        }
        User target = get(targetId);
        if (target.getRole() == Role.ADMIN && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("The last administrator cannot be deleted.");
        }
        Wallet wallet = target.getWallet();
        boolean hasValue = wallet != null
            && (wallet.getCashBalance().signum() > 0 || !wallet.getHoldings().isEmpty());
        if (hasValue && !force) {
            throw new BusinessRuleException(
                "User has a non-zero balance or open holdings. Re-send with force=true to delete anyway.");
        }
        userRepository.delete(target);   // cascades to wallet, holdings, operations, recovery codes
        log.warn("ADMIN DELETE actor={} target={} force={}", actorId, targetId, force);
    }

    @Transactional
    public User changeRole(UUID actorId, UUID targetId, Role role) {
        User target = get(targetId);
        if (target.getRole() == Role.ADMIN && role != Role.ADMIN
            && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("The last administrator cannot be demoted.");
        }
        target.assignRole(role);
        log.warn("ADMIN ROLE CHANGE actor={} target={} newRole={}", actorId, targetId, role);
        return target;
    }
}
```

Add `void assignRole(Role role)` to `User`.

- [ ] **Step 5: Implement the DTOs with CPF masking**

An admin list is a common place to leak PII. `cpfMasked` shows only the last five digits:

```java
public record AdminUserResponse(
    UUID id, String name, String email, String cpfMasked, String role,
    boolean twoFactorEnabled, BigDecimal cashBalance, int holdingCount, Instant createdAt) {

    public static AdminUserResponse from(User user) {
        Wallet wallet = user.getWallet();
        return new AdminUserResponse(
            user.getId(), user.getName(), user.getEmail(), mask(user.getCpf()),
            user.getRole().name(), user.isTwoFactorEnabled(),
            wallet == null ? Money.ZERO_CASH : wallet.getCashBalance(),
            wallet == null ? 0 : wallet.getHoldings().size(),
            user.getCreatedAt());
    }

    /** "52998224725" -> "***.***.247-25" */
    private static String mask(String cpf) {
        return "***.***.%s-%s".formatted(cpf.substring(6, 9), cpf.substring(9, 11));
    }
}
```

`ChangeRoleRequest(@NotNull Role role)`.

- [ ] **Step 6: Implement `AdminUserController`**

Class-level `@RestController @RequestMapping("/api/v1/admin/users") @PreAuthorize("hasRole('ADMIN')")` so no individual method can be left unguarded by accident. `DELETE` returns `204 No Content` and takes `@RequestParam(defaultValue = "false") boolean force`. Every method passes `SecurityUtils.currentUserId()` as the actor.

- [ ] **Step 7: Implement `AdminSeeder`**

Flyway cannot compute a BCrypt hash, so the first admin is created by an application runner that uses the real `PasswordEncoder`.

```java
@Component
@ConditionalOnProperty(name = "app.admin.seed-enabled", havingValue = "true")
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String cpf;

    // constructor reads app.admin.{email,password,cpf}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userRepository.save(User.create(
            "Administrator", email.toLowerCase(), passwordEncoder.encode(password), cpf, Role.ADMIN));
        log.warn("Seeded administrator account {} — change this password before any real deployment", email);
    }
}
```

```yaml
app:
  admin:
    seed-enabled: ${ADMIN_SEED_ENABLED:true}
    email: ${ADMIN_EMAIL:admin@criptoativos.local}
    password: ${ADMIN_PASSWORD:change-me-immediately}
    cpf: ${ADMIN_CPF:16899535009}
```

The README (Task 16) must state plainly that `ADMIN_PASSWORD` has to be set for any deployment and that `ADMIN_SEED_ENABLED=false` disables seeding entirely.

- [ ] **Step 8: Extend `TestFixtures` with `adminToken()`, `adminId()`, and `userToken()`**

These are used by `AdminUserIT` and are worth centralising — several later tests need an admin token too.

- [ ] **Step 9: Run `./mvnw verify`, then commit**

```powershell
git add -A
git commit -m "feat: add admin user management with deletion and demotion guardrails"
```

---

## Task 14: OpenAPI documentation and Actuator

**Files:**
- Create: `config/OpenApiConfig.java`, `docs/api.http`
- Modify: controllers (add `@Tag`, `@Operation`, `@ApiResponse` where the meaning is not obvious)
- Create: `src/test/java/com/criptoativos/OpenApiIT.java`

**Interfaces:**
- Consumes: every controller from Tasks 4–13.
- Produces: `/swagger-ui.html`, `/v3/api-docs`, a `bearerAuth` security scheme so Swagger UI's "Authorize" button works.

- [ ] **Step 1: Implement `OpenApiConfig`**

```java
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    OpenAPI criptoAtivosOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("CriptoAtivos API")
                .description("Crypto portfolio management: accounts, wallets, live prices, and trading.")
                .version("v1")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

- [ ] **Step 2: Write `OpenApiIT`** — assert `/v3/api-docs` returns 200 without a token and that `$.paths['/api/v1/transactions/buy']` exists. This makes the docs a build-breaking contract instead of decoration.

- [ ] **Step 3: Write `docs/api.http`** — a runnable request collection (IntelliJ HTTP Client / VS Code REST Client) covering register → login → deposit → buy → wallet → sell → history, with `@accessToken` captured from the login response.

- [ ] **Step 4: Run `./mvnw verify`, open `http://localhost:8080/swagger-ui.html` to confirm visually, commit**

```powershell
git add -A
git commit -m "docs: publish OpenAPI spec and Swagger UI with bearer auth"
```

---

## Task 15: Hardening, Docker, CI, and quality gates

**Files:**
- Create: `Dockerfile`, `.dockerignore`, `.github/workflows/ci.yml`, `config/WebConfig.java`, `.env.example`
- Modify: `compose.yaml` (add the `api` service), `pom.xml` (JaCoCo + Spotless), `application.yml`, `SecurityConfig.java`

**Interfaces:**
- Consumes: the whole application.
- Produces: `docker compose up` runs API + database; `./mvnw verify` enforces coverage and formatting; CI runs on every push and PR.

- [ ] **Step 1: Add CORS and security headers**

`WebConfig` reads an allow-list from `app.cors.allowed-origins` (default `http://localhost:3000`). In `SecurityConfig`, add:
```java
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)))
.cors(Customizer.withDefaults())
```

- [ ] **Step 2: Rate-limit the auth endpoints**

A `Bucket4j`-free approach keeps dependencies down: an in-memory `ConcurrentHashMap<String, Window>` filter allowing 10 requests per IP per minute to `/api/v1/auth/**`, returning `429` with a `ProblemDetail`. Test it with 11 rapid `MockMvc` calls asserting the 11th is `429`.

- [ ] **Step 3: Write the `Dockerfile`**

```dockerfile
# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

Non-root user, JRE-only runtime layer, and a real health check — the three things reviewers look for.

- [ ] **Step 4: Add the `api` service to `compose.yaml`**

```yaml
  api:
    build: .
    depends_on:
      postgres: {condition: service_healthy}
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/criptoativos
      DB_USER: criptoativos
      DB_PASSWORD: criptoativos
      JWT_PUBLIC_KEY: file:/run/secrets/public.pem
      JWT_PRIVATE_KEY: file:/run/secrets/private.pem
      ENCRYPTION_PASSWORD: ${ENCRYPTION_PASSWORD:-dev-only-change-me}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-change-me-immediately}
    volumes:
      - ./src/main/resources/certs:/run/secrets:ro
    ports: ["8080:8080"]
```

- [ ] **Step 5: Add JaCoCo and Spotless to `pom.xml`**

JaCoCo with a `check` rule at **80% line coverage** on `com.criptoativos.*`, excluding `**/dto/**`, `**/config/**`, and `CriptoAtivosApplication`. Spotless with `googleJavaFormat()` and `<goal>check</goal>` bound to `validate`.

- [ ] **Step 6: Write `.github/workflows/ci.yml`**

```yaml
name: CI
on:
  push: {branches: [main]}
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Generate JWT signing keys
        run: |
          mkdir -p src/main/resources/certs
          openssl genrsa -out src/main/resources/certs/private.pem 2048
          openssl rsa -in src/main/resources/certs/private.pem -pubout -out src/main/resources/certs/public.pem
      - name: Build, test, and verify coverage
        env:
          ENCRYPTION_PASSWORD: ci-only-encryption-password
          ENCRYPTION_SALT: 5c0744940b5c369b
        run: ./mvnw -B verify
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: jacoco-report
          path: target/site/jacoco/
```

The GitHub Ubuntu runner has Docker, so Testcontainers works unmodified.

- [ ] **Step 7: Run the full gate locally, then commit**

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd clean verify
docker compose up --build -d
curl http://localhost:8080/actuator/health
docker compose down
git add -A
git commit -m "chore: add docker packaging, CI pipeline, and coverage gates"
```

---

## Task 16: README and architecture documentation

The single highest-leverage task for the resume goal — most reviewers read only this.

**Files:**
- Create: `README.md`, `docs/ARCHITECTURE.md`, `LICENSE`

- [ ] **Step 1: Write `README.md`** containing, in order:
  1. One-line description and badges:
     ```markdown
     [![CI](https://github.com/alvarogalhardo/CriptoAtivos/actions/workflows/ci.yml/badge.svg)](https://github.com/alvarogalhardo/CriptoAtivos/actions/workflows/ci.yml)
     ![Java](https://img.shields.io/badge/Java-21-orange)
     ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
     ![License](https://img.shields.io/badge/license-MIT-blue)
     ```
  2. **Quickstart** — the exact commands: generate the RSA keypair, `docker compose up --build`, open `http://localhost:8080/swagger-ui.html`
  3. **Screenshot** of Swagger UI (capture it while the stack is up)
  4. **Features** — JWT auth with RS256, TOTP two-factor with recovery codes, RFC 7807 errors, live CoinGecko prices, exchange-side inventory, portfolio P&L, admin user management, pessimistic-locked trading
  5. **API reference table** — method, path, auth required, description
  6. **Architecture** — package-by-feature diagram plus the request/service/repository flow
  7. **Design decisions**, each one sentence with its reason:
     - `BigDecimal` everywhere, never `double` — binary floating point cannot represent `0.1`, and money must not drift
     - Flyway owns the schema; Hibernate runs `ddl-auto: validate` — schema changes are reviewable and repeatable
     - JPA single-table inheritance retained for `Asset` and `Operation` — the original coursework hierarchies, now with real subclasses (`CashOperation` alongside `Transaction`)
     - Pessimistic locks on wallet and inventory, always in that order — money is the one place where retry-on-conflict is worse than serialising, and a fixed order rules out deadlock
     - TOTP secrets encrypted at rest, recovery codes BCrypt-hashed — a database dump alone must not mint valid codes
     - A 2FA challenge token is a distinct authority, never `ROLE_USER` — a half-authenticated session must not reach protected endpoints
     - `PriceProvider` port — the domain never depends on CoinGecko, and every test runs offline
     - No Lombok — the project opens and builds in any IDE with no plugin
     - Identical login failure messages, and the password encoder runs even for unknown emails — no user enumeration by response or by timing
  8. **Configuration** — the full environment-variable table, with `ADMIN_PASSWORD`, `ENCRYPTION_PASSWORD`, and the JWT key paths called out as **must change before deploying**
  9. **Testing** — how to run, what `*Test` vs `*IT` mean, the coverage gate, and that Docker must be running
  10. **Project history** — a short, honest note that this began as an academic console application, linking the `v0-academic-baseline` tag and the `archive/console-draft` branch. Reviewers respect a visible before/after far more than a project that pretends it was always this way.

- [ ] **Step 2: Write `docs/ARCHITECTURE.md`** — the layer diagram, the entity-relationship diagram (Mermaid), the buy-transaction sequence diagram showing both locks in order (Mermaid), the 2FA login state machine (Mermaid), and the concurrency model.

- [ ] **Step 3: Add an MIT `LICENSE`**

- [ ] **Step 4: Set the repository description and topics on GitHub**

An empty repo sidebar is the first thing a reviewer sees. Via `gh`:
```powershell
gh repo edit alvarogalhardo/CriptoAtivos --description "Crypto portfolio management REST API — Spring Boot 3, PostgreSQL, JWT + TOTP 2FA, Docker" --add-topic java --add-topic spring-boot --add-topic rest-api --add-topic postgresql --add-topic jwt --add-topic docker --add-topic testcontainers
```

- [ ] **Step 5: Final verification, then open and merge the pull request**

```powershell
.\mvnw.cmd clean verify
git add -A
git commit -m "docs: add README, architecture guide, and license"
git push
gh pr create --base main --head feat/production-revamp --title "Production-grade rewrite: Spring Boot REST API" --body "Rewrites the academic console application as a documented, tested, containerized REST API. Baseline preserved at tag v0-academic-baseline."
```

Wait for CI to go green on the PR, then merge and tag:
```powershell
gh pr checks --watch
gh pr merge --squash --delete-branch
git checkout main
git pull
git tag -a v1.0.0 -m "Production-grade REST API"
git push origin v1.0.0
```

Merging through a PR with a green CI check is the artifact a reviewer actually looks for — it shows the workflow, not just the code.

---

## Verification

Run these end-to-end after Task 16 to confirm the whole system works, not just the unit tests. All commands run from `C:\Users\MICRO\Desktop\CriptoAtivos-api`.

**1. Full build from clean**
```powershell
.\mvnw.cmd clean verify
```
Expect: `BUILD SUCCESS`, all `*Test` and `*IT` green, JaCoCo ≥ 80%, Spotless clean.

**2. Containerized stack**
```powershell
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health
```
Expect: both services `healthy`; health returns `{"status":"UP"}`.

**3. Full user journey over HTTP** — run `docs/api.http` top to bottom, or by hand:
```powershell
$body = '{"name":"Ana Maria","email":"ana@example.com","password":"s3cret-passw0rd","cpf":"52998224725"}'
curl -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d $body
$token = (curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"email":"ana@example.com","password":"s3cret-passw0rd"}' | ConvertFrom-Json).accessToken
curl -X POST http://localhost:8080/api/v1/wallet/deposits -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"amount":10000.00}'
curl -X POST http://localhost:8080/api/v1/transactions/buy -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"symbol":"BTC","quantity":0.01}'
curl http://localhost:8080/api/v1/wallet -H "Authorization: Bearer $token"
```
Expect: `201`, a JWT, balance `10000.00`, a BTC holding priced from live CoinGecko data, and a wallet response with non-zero `marketValue` and a computed `unrealisedPnl`.

**4. The original bugs are actually fixed** — confirm each by hand:
- Buy more than the balance allows → `422`, and `GET /api/v1/wallet` is byte-identical to before the attempt
- Buy BTC twice → `holdings` has **one** entry with the summed quantity
- Sell after a price refresh → proceeds use the new price, not the purchase price
- `GET /api/v1/wallet` without a token → `401`, not a stack trace
- Post `{"amount":"abc"}` → `400` `ProblemDetail`, and the service stays up

**5. Two-factor round trip**
```powershell
curl -X POST http://localhost:8080/api/v1/auth/2fa/setup -H "Authorization: Bearer $token"
```
Scan the returned `provisioningUri` with any authenticator app, then confirm with a live code, log in again, and verify. Expect: login now returns `twoFactorRequired: true` and no `accessToken`; the challenge token is rejected by `GET /api/v1/users/me` with **403**; `/2fa/verify` with a valid code returns a working token; one recovery code works exactly once.

**6. Inventory and admin**
```powershell
curl http://localhost:8080/api/v1/assets/BTC/inventory
curl -X PUT http://localhost:8080/api/v1/assets/BTC/inventory -H "Authorization: Bearer $adminToken" -H "Content-Type: application/json" -d '{"availableQuantity":"0.001"}'
curl -X POST http://localhost:8080/api/v1/transactions/buy -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"symbol":"BTC","quantity":1}'
```
Expect the buy to fail with `422 Insufficient inventory`, and the wallet balance to be unchanged afterwards. Then check the admin guardrails: an admin deleting their own id → `422`; deleting a funded user without `force` → `422`; demoting the only admin → `422`.

**7. Security spot-check**
```powershell
curl http://localhost:8080/api/v1/wallet
curl -X POST http://localhost:8080/api/v1/assets -H "Authorization: Bearer $token" -H "Content-Type: application/json" -d '{"symbol":"DOGE","name":"Dogecoin","currentPrice":0.15,"externalId":"dogecoin"}'
curl http://localhost:8080/api/v1/admin/users -H "Authorization: Bearer $token"
```
Expect `401`, `403`, `403`. Also confirm no response anywhere contains `passwordHash`, `twoFactorSecret`, or an unmasked `cpf`, and that `git log -p --all` contains no `.pem` file:
```powershell
git log --all --name-only --pretty=format: | Select-String -Pattern "\.pem$"
```
Expect: no output.

**8. Clean-clone check** — the reviewer's actual experience:
```powershell
git clone https://github.com/alvarogalhardo/CriptoAtivos.git C:\Users\MICRO\AppData\Local\Temp\criptoativos-check
cd C:\Users\MICRO\AppData\Local\Temp\criptoativos-check
# follow README quickstart verbatim, touching nothing else
```
Expect: the README's steps work with no undocumented prerequisites. If they don't, the README is wrong — fix it, not the reader.

---

## Notes and risks

- **Docker is required** for the test suite (Testcontainers) and for `compose up`. Docker Desktop must be running before `./mvnw verify`. If it isn't available, the fallback is switching `AbstractIT` to an embedded Postgres — but Testcontainers is the better resume signal, so keep it.
- **CoinGecko's free tier** is rate-limited (roughly 10–30 calls/min). The 5-minute cron plus one batched call per refresh stays well inside it. An outage degrades to stale prices, never to failing requests — `CoinGeckoPriceProviderTest.returnsAnEmptyMapWhenTheProviderIsDown` pins that behaviour.
- **Spring Boot version:** pin the newest **3.5.x** at Task 1 Step 7. Do not adopt 4.x mid-plan; the security and JPA configuration in Tasks 5–10 targets the 3.5 API.
- **Task 4 forward-references `Wallet`.** Create the stub described in the Task 4 note, or merge Tasks 4 and 9 if the executor prefers fewer partial files.
- **`gen_random_uuid()` requires `pgcrypto`**, created in `V1__baseline.sql`. On Postgres 13+ it is built in, but the `create extension` is harmless and keeps older versions working.
- **Two divergent codebases.** The plan builds on the GitHub repo. The local `Desktop\CriptoAtivos` folder is a *different, older* draft with no Git history — Task 1 Step 6 archives it onto a branch. Do not edit it, and do not confuse the two working directories.
- **Task 6 changes the login contract.** `POST /auth/login` returns `LoginResponse`, not `TokenResponse`. Every test written in Task 5 that reads `$.accessToken` still passes only while 2FA is disabled — which is the default for a freshly registered user, so Task 5's `AuthenticationIT` needs no change. Confirm this rather than assuming it.
- **The challenge-token privilege hole is easy to reintroduce.** Any later `.anyRequest().authenticated()` in `SecurityConfig` silently grants a half-authenticated 2FA challenge token full access. Keep the explicit `hasAnyRole("USER","ADMIN")` and keep the `TwoFactorIT` test that asserts 403.
- **Lock ordering is wallet → inventory, everywhere.** Reversing it in any future code path deadlocks under concurrency, and the failure is intermittent and hard to reproduce. `TransactionService` should stay the only class that holds both locks.
- **`AdminSeeder` runs by default.** `ADMIN_SEED_ENABLED=true` with a default password is right for a portfolio demo and wrong for anything real. The README must say so, and the seeder logs a warning every time it fires.
