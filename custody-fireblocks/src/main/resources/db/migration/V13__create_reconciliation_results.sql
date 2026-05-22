CREATE TABLE reconciliation_results (
    id UUID PRIMARY KEY,
    vault_id UUID NOT NULL REFERENCES vaults(id),
    currency VARCHAR(50) NOT NULL,
    protocol VARCHAR(50) NOT NULL,
    internal_balance NUMERIC(36, 18) NOT NULL,
    fireblocks_balance NUMERIC(36, 18) NOT NULL,
    drift NUMERIC(36, 18) NOT NULL,
    absolute_drift NUMERIC(36, 18) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tolerance_used NUMERIC(36, 18) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reconciliation_results_vault_created ON reconciliation_results(vault_id, created_at);
CREATE INDEX idx_reconciliation_results_mismatched ON reconciliation_results(status) WHERE status = 'MISMATCHED';
