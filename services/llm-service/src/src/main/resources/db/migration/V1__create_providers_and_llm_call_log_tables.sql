-- Create providers table
CREATE TABLE providers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_type VARCHAR(50) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create unique constraint for providers
CREATE UNIQUE INDEX unique_provider_apikey ON providers(provider_type, api_key);

-- Create llm_call_log table
CREATE TABLE llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT,
    provider_type VARCHAR(50) NOT NULL,
    request_content TEXT,
    response_content TEXT,
    tokens_used INTEGER,
    response_time_ms BIGINT,
    status VARCHAR(20),
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add foreign key constraint
ALTER TABLE llm_call_log ADD CONSTRAINT fk_provider FOREIGN KEY (provider_id) REFERENCES providers(id);

-- Create indexes for better performance
CREATE INDEX idx_llm_call_log_provider_type ON llm_call_log(provider_type);
CREATE INDEX idx_llm_call_log_created_at ON llm_call_log(created_at);
CREATE INDEX idx_llm_call_log_status ON llm_call_log(status);
CREATE INDEX idx_providers_provider_type ON providers(provider_type);
