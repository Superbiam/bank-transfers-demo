ALTER TABLE cards DROP CONSTRAINT cards_card_status_check;
ALTER TABLE cards ADD CONSTRAINT ck_cards_status
    CHECK (card_status IN ('ACTIVE', 'BLOCKED', 'EXPIRED'));

ALTER TABLE phones DROP CONSTRAINT phones_phone_number_check;
ALTER TABLE phones ADD CONSTRAINT ck_phones_number
    CHECK (phone_number ~ '^\+[1-9][0-9]{6,14}$');

ALTER TABLE transactions ADD CONSTRAINT ck_tx_status
    CHECK (transaction_status IN ('COMPLETED'));
ALTER TABLE transactions ADD CONSTRAINT ck_tx_rate CHECK (
    (rate IS NULL AND debit_currency = credit_currency)
    OR (rate IS NOT NULL AND rate > 0 AND debit_currency <> credit_currency));

ALTER TABLE cards ADD COLUMN pan_length smallint NOT NULL
    CHECK (pan_length BETWEEN 13 AND 19);
ALTER TABLE cards ADD COLUMN version bigint NOT NULL DEFAULT 0;

ALTER TABLE accounts DROP COLUMN closed_at;