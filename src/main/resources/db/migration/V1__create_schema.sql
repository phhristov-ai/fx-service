CREATE TABLE clients (
	id BIGSERIAL PRIMARY KEY,
	client_id VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE balances (
	id BIGSERIAL PRIMARY KEY,
	client_id BIGINT NOT NULL,
	currency VARCHAR(3) NOT NULL,
	amount NUMERIC(19, 4) NOT NULL,
	version BIGINT NOT NULL DEFAULT 0,

	CONSTRAINT fk_balances_client
		FOREIGN KEY (client_id)
			REFERENCES clients(id),

	CONSTRAINT uk_balances_client_currency
		UNIQUE (client_id, currency)
);

CREATE TABLE conversions (
	id BIGSERIAL PRIMARY KEY,
	transaction_id UUID NOT NULL UNIQUE,
	client_id BIGINT NOT NULL,
	source_currency VARCHAR(3) NOT NULL,
	source_amount NUMERIC(19, 4) NOT NULL,
	target_currency VARCHAR(3) NOT NULL,
	target_amount NUMERIC(19, 4) NOT NULL,
	rate NUMERIC(19, 10) NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,

	source_balance_after NUMERIC(19, 4) NOT NULL,
	target_balance_after NUMERIC(19, 4) NOT NULL,

	CONSTRAINT fk_conversions_client
		FOREIGN KEY (client_id)
			REFERENCES clients(id)
);

CREATE TABLE idempotency_keys (
	id BIGSERIAL PRIMARY KEY,
	client_id VARCHAR(100) NOT NULL,
	idempotency_key VARCHAR(255) NOT NULL,
	transaction_id UUID NOT NULL,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,

	CONSTRAINT uk_idempotency_client_key
		UNIQUE (client_id, idempotency_key)
);

CREATE INDEX idx_idempotency_transaction_id
	ON idempotency_keys(transaction_id);