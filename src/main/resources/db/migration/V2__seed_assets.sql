-- A starter catalogue so the API is usable immediately after `docker compose up`.
-- Prices start at 0 and are filled in by the first price refresh (see PriceRefreshJob);
-- external_id is the CoinGecko coin id.

insert into assets (asset_type, symbol, name, description, current_price, external_id) values
    ('CRYPTO', 'BTC', 'Bitcoin',  'The first decentralised cryptocurrency.',            0, 'bitcoin'),
    ('CRYPTO', 'ETH', 'Ethereum', 'Smart-contract platform and its native token, Ether.', 0, 'ethereum'),
    ('CRYPTO', 'SOL', 'Solana',   'High-throughput proof-of-stake blockchain.',         0, 'solana'),
    ('CRYPTO', 'ADA', 'Cardano',  'Proof-of-stake blockchain with a research-led design.', 0, 'cardano'),
    ('CRYPTO', 'XRP', 'XRP',      'Digital asset for cross-border settlement.',         0, 'ripple')
on conflict (symbol) do nothing;

-- Every asset needs an inventory row, or the first buy fails looking for one.
insert into asset_inventory (asset_id, available_quantity)
select id, 1000 from assets
on conflict (asset_id) do nothing;
