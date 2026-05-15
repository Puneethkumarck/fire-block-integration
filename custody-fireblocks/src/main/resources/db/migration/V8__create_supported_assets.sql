CREATE TABLE supported_assets (
    id                  UUID PRIMARY KEY,
    currency            VARCHAR(10) NOT NULL,
    protocol            VARCHAR(20) NOT NULL,
    fireblocks_asset_id VARCHAR(50) NOT NULL UNIQUE,
    taggable            BOOLEAN NOT NULL DEFAULT FALSE,
    utxo                BOOLEAN NOT NULL DEFAULT FALSE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_supported_assets_currency_protocol UNIQUE (currency, protocol)
);

INSERT INTO supported_assets (id, currency, protocol, fireblocks_asset_id, taggable, utxo) VALUES
    (gen_random_uuid(), 'BTC',  'BTC', 'BTC',  false, true),
    (gen_random_uuid(), 'ETH',  'ETH', 'ETH',  false, false),
    (gen_random_uuid(), 'SOL',  'SOL', 'SOL',  false, false),
    (gen_random_uuid(), 'EURC', 'ETH', 'EURC', false, false);
