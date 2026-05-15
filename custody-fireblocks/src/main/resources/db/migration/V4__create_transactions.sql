CREATE TABLE transactions (
    id                          UUID PRIMARY KEY,
    external_tx_id              VARCHAR(255) NOT NULL UNIQUE,
    fireblocks_transaction_id   VARCHAR(255),
    status                      VARCHAR(20) NOT NULL,
    fireblocks_status           VARCHAR(50),
    fireblocks_sub_status       VARCHAR(50),
    asset_id                    VARCHAR(50) NOT NULL,
    currency                    VARCHAR(10) NOT NULL,
    protocol                    VARCHAR(20) NOT NULL,
    amount                      NUMERIC(36, 18) NOT NULL,
    source_vault_id             VARCHAR(255) NOT NULL,
    destination_address         VARCHAR(256) NOT NULL,
    fee_level                   VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    treat_as_gross_amount       BOOLEAN NOT NULL DEFAULT FALSE,
    tx_hash                     VARCHAR(128),
    note                        VARCHAR(500),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    version                     BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_transactions_external_tx_id ON transactions(external_tx_id);
CREATE INDEX idx_transactions_fireblocks_tx_id ON transactions(fireblocks_transaction_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_source_vault_id ON transactions(source_vault_id);
