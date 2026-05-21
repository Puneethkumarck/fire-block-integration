CREATE TABLE fund_allocations (
    id UUID PRIMARY KEY,
    allocation_id VARCHAR(255) NOT NULL,
    vault_id UUID NOT NULL,
    fireblocks_vault_id VARCHAR(255) NOT NULL,
    asset_id VARCHAR(50) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    amount NUMERIC(36, 18) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_fund_allocations_allocation_id UNIQUE (allocation_id),
    CONSTRAINT fk_fund_allocations_vault FOREIGN KEY (vault_id) REFERENCES vaults(id),
    CONSTRAINT fk_fund_allocations_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);

CREATE INDEX idx_fund_allocations_status_created ON fund_allocations (status, created_at)
    WHERE status IN ('LOCKED', 'PENDING');
CREATE INDEX idx_fund_allocations_transaction_id ON fund_allocations (transaction_id);
