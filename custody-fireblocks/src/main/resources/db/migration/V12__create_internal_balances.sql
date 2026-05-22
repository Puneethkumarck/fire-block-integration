CREATE TABLE internal_balances (
    id UUID PRIMARY KEY,
    vault_id UUID NOT NULL REFERENCES vaults(id),
    currency VARCHAR(50) NOT NULL,
    protocol VARCHAR(50) NOT NULL,
    balance NUMERIC(36, 18) NOT NULL,
    last_transaction_id UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_internal_balance_vault_currency_protocol UNIQUE (vault_id, currency, protocol)
);
