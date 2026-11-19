CREATE TABLE clients ( 
    id uuid PRIMARY KEY,
    last_name text NOT NULL, first_name text NOT NULL, middle_name text,
    email text NOT NULL,
    birth_date date NOT NULL CHECK (birth_date <= CURRENT_DATE),
    created_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz
);
CREATE UNIQUE INDEX ux_clients_email ON clients (lower(email))
    WHERE deleted_at IS NULL;

CREATE TABLE phones (
    id uuid PRIMARY KEY,
    client_id uuid NOT NULL REFERENCES clients(id),
    phone_number text NOT NULL CHECK (phone_number ~ '^\+[1-9][0-9]{7,14}$'),
    phone_type text NOT NULL CHECK  (phone_type IN ('MOBILE', 'HOME', 'WORK')),
    is_primary boolean NOT NULL DEFAULT false,
    UNIQUE (client_id, phone_number)
);
CREATE UNIQUE INDEX ux_phones_primary ON phones (client_id) WHERE is_primary;

CREATE TABLE accounts (
    id uuid PRIMARY KEY,
    client_id uuid NOT NULL REFERENCES clients(id),
    account_number text NOT NULL UNIQUE,
    currency char(3) NOT NULL,
    balance numeric(19, 4) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    account_status text NOT NULL CHECK (account_status IN ('ACTIVE', 'CLOSED')),
    version bigint NOT NULL DEFAULT 0,
    closed_at timestamptz
);
CREATE INDEX ix_accounts_client_cur ON accounts (client_id, currency);

CREATE TABLE cards (
    id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES accounts(id),
    pan_encrypted bytea NOT NULL,
    pan_hmac bytea NOT NULL UNIQUE,
    card_bin char(6) NOT NULL,
    card_last4 char(4) NOT NULL,
    expires_on date NOT NULL,
    card_type text NOT NULL CHECK (card_type IN ('DEBIT', 'CREDIT')),
    card_status text NOT NULL CHECK (card_status IN ('ACTIVE', 'BLOCKED', 'EXPPIRED'))
);
CREATE INDEX  ix_cards_account ON cards (account_id);

CREATE TABLE card_status_history (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    card_id uuid NOT NULL REFERENCES cards(id),
    from_status text NOT NULL, to_status text NOT NULL,
    changed_by text NOT NULL,
    reason text NOT NULL,
    changed_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id uuid PRIMARY KEY,
    from_account_id uuid NOT NULL REFERENCES accounts(id),
    to_account_id uuid NOT NULL REFERENCES accounts(id),
    debit_amount numeric(19, 4) NOT NULL CHECK (debit_amount > 0),
    debit_currency char(3) NOT NULL,
    credit_amount numeric(19, 4) NOT NULL CHECK (credit_amount > 0),
    credit_currency char(3) NOT NULL,
    rate numeric(19, 8), -- NULL, если не было конвертации
    transaction_status text NOT NULL,
    idempotency_key text NOT NULL,
    comment text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CHECK (from_account_id <> to_account_id)
);
CREATE INDEX ix_tx_from ON transactions (from_account_id, created_at DESC, id DESC);
CREATE INDEX ix_tx_to ON transactions (to_account_id, created_at DESC, id DESC);

CREATE TABLE idempotency_keys (
    user_id text NOT NULL, idempotency_key text NOT NULL,
    request_hash bytea NOT NULL,
    transaction_id uuid REFERENCES transactions(id),
    expires_at timestamptz NOT NULL,
    PRIMARY KEY (user_id, idempotency_key)
);
CREATE INDEX ix_idem_expires ON idempotency_keys (expires_at);

CREATE FUNCTION forbid_modification() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% on % is not allowed: table is append-only', TG_OP, TG_TABLE_NAME
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_transactions_immutable
    BEFORE UPDATE OR DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION forbid_modification();

CREATE TRIGGER trg_card_status_history_immutable
    BEFORE UPDATE OR DELETE ON card_status_history
    FOR EACH ROW EXECUTE FUNCTION forbid_modification();

CREATE TRIGGER trg_transactions_no_truncate
    BEFORE TRUNCATE ON transactions
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_modification();

CREATE TRIGGER trg_card_status_history_no_truncate
    BEFORE TRUNCATE ON card_status_history
    FOR EACH STATEMENT EXECUTE FUNCTION forbid_modification();