CREATE TABLE vaults (
    id              UUID PRIMARY KEY,
    fireblocks_vault_id VARCHAR(255),
    customer_ref_id VARCHAR(128) NOT NULL UNIQUE,
    name            VARCHAR(128) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_vaults_customer_ref_id ON vaults(customer_ref_id);
CREATE INDEX idx_vaults_status ON vaults(status);
