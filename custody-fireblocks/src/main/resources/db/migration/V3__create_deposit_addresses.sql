CREATE TABLE deposit_addresses (
    id              UUID PRIMARY KEY,
    wallet_asset_id UUID NOT NULL REFERENCES wallet_assets(id),
    address         VARCHAR(256) NOT NULL,
    tag             VARCHAR(128),
    legacy_address  VARCHAR(256),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_deposit_addresses_wallet_asset_id ON deposit_addresses(wallet_asset_id);
