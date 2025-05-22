CREATE TABLE connections (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    db_type VARCHAR(50) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    database_name VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    password_encrypted VARCHAR(255) NOT NULL,
    ssl BOOLEAN NOT NULL DEFAULT FALSE,
    connection_timeout BIGINT NOT NULL DEFAULT 30000,
    idle_timeout BIGINT NOT NULL DEFAULT 600000,
    max_pool_size INTEGER NOT NULL DEFAULT 10,
    parameters JSONB NOT NULL DEFAULT '{}',
    created_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
); 