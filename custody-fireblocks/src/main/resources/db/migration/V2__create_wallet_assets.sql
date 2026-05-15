CREATE TABLE wallet_assets (
    id                  UUID PRIMARY KEY,
    vault_id            UUID NOT NULL REFERENCES vaults(id),
    currency            VARCHAR(10) NOT NULL,
    protocol            VARCHAR(20) NOT NULL,
    fireblocks_asset_id VARCHAR(50) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT NOT NULL DEFAULT 0,
    UNIQUE (vault_id, currency, protocol)
);

CREATE INDEX idx_wallet_assets_vault_id ON wallet_assets(vault_id);
CREATE INDEX idx_wallet_assets_currency_protocol ON wallet_assets(currency, protocol);
