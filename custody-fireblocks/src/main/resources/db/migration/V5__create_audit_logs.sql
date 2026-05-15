CREATE TABLE audit_logs (
    id                      UUID PRIMARY KEY,
    timestamp               TIMESTAMP WITH TIME ZONE NOT NULL,
    actor                   VARCHAR(255) NOT NULL,
    operation               VARCHAR(50) NOT NULL,
    resource_id             VARCHAR(255) NOT NULL,
    fireblocks_request_id   VARCHAR(255),
    status                  VARCHAR(20) NOT NULL,
    details                 JSONB
);

CREATE INDEX idx_audit_logs_resource_id ON audit_logs(resource_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

CREATE OR REPLACE FUNCTION prevent_audit_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs table is immutable: % not allowed', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER no_audit_update_delete
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
