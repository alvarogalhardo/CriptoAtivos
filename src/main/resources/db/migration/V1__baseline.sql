-- Baseline schema for CriptoAtivos.
--
-- Derived from the academic Oracle DDL preserved in docs/legacy/DDL2-oracle.sql,
-- with the flaws corrected:
--   * DOUBLE -> NUMERIC, because money must not drift
--   * AUTO_INCREMENT integer keys -> UUID
--   * DATE -> timestamptz
--   * holdings and asset_inventory get real tables (the original had Java classes
--     CarteiraCriptoAtivos and EstoqueCriptoAtivos with no persistence at all)

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Users and authentication
-- ---------------------------------------------------------------------------

create table users (
    id                      uuid primary key default gen_random_uuid(),
    name                    varchar(120) not null,
    email                   varchar(255) not null unique,
    password_hash           varchar(72)  not null,
    cpf                     char(11)     not null unique,
    role                    varchar(20)  not null,
    two_factor_enabled      boolean      not null default false,
    two_factor_secret       varchar(120),
    two_factor_confirmed_at timestamptz,
    created_at              timestamptz  not null default now(),
    updated_at              timestamptz  not null default now(),
    constraint chk_users_cpf_digits check (cpf ~ '^[0-9]{11}$'),
    constraint chk_users_role check (role in ('USER', 'ADMIN'))
);

-- Single-use backup codes, stored hashed exactly like passwords.
create table user_recovery_codes (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references users (id) on delete cascade,
    code_hash  varchar(72) not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);

create index idx_recovery_codes_user on user_recovery_codes (user_id) where used_at is null;

-- ---------------------------------------------------------------------------
-- Assets
-- ---------------------------------------------------------------------------

create table assets (
    id               uuid primary key default gen_random_uuid(),
    asset_type       varchar(31)   not null,
    symbol           varchar(20)   not null unique,
    name             varchar(120)  not null,
    description      varchar(500),
    current_price    numeric(19,8) not null default 0,
    daily_change_pct numeric(9,4),
    external_id      varchar(60),
    price_updated_at timestamptz,
    created_at       timestamptz   not null default now(),
    version          bigint        not null default 0,
    constraint chk_assets_price_non_negative check (current_price >= 0)
);

-- Exchange-side available supply: the EstoqueCriptoAtivos concept, made real.
-- Kept out of `assets` so the periodic price refresh never contends with the
-- row lock the trading path takes.
create table asset_inventory (
    asset_id           uuid primary key references assets (id) on delete cascade,
    available_quantity numeric(19,8) not null default 0,
    updated_at         timestamptz   not null default now(),
    version            bigint        not null default 0,
    constraint chk_inventory_non_negative check (available_quantity >= 0)
);

-- ---------------------------------------------------------------------------
-- Wallets and holdings
-- ---------------------------------------------------------------------------

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
    constraint chk_holdings_quantity_positive check (quantity > 0),
    constraint chk_holdings_avg_cost_non_negative check (average_cost >= 0)
);

-- ---------------------------------------------------------------------------
-- Operations (single-table inheritance: Transaction | CashOperation)
-- ---------------------------------------------------------------------------

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
    amount           numeric(19,2),
    constraint chk_operations_type check (operation_type in ('TRANSACTION', 'CASH')),
    constraint chk_operations_transaction_type check (transaction_type is null or transaction_type in ('BUY', 'SELL')),
    constraint chk_operations_cash_type check (cash_type is null or cash_type in ('DEPOSIT', 'WITHDRAWAL')),
    constraint chk_operations_quantity_positive check (quantity is null or quantity > 0),
    constraint chk_operations_amount_positive check (amount is null or amount > 0)
);

create index idx_operations_user_occurred on operations (user_id, occurred_at desc);
create index idx_operations_asset on operations (asset_id);
