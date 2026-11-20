package kg.yarrrkady.banktransfersdemo.domain.entity.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;

public record Transaction(
    UUID id, UUID fromAccountId, UUID toAccountId,
    Money debitAmount, Money creditAmount, BigDecimal rate,
    TransactionStatus status, String idempotencyKey,
    Instant createdAt, String comment) {

    public Transaction {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fromAccountId, "fromAccountId");
        Objects.requireNonNull(toAccountId, "toAccountId");
        Objects.requireNonNull(debitAmount, "debitAmount");
        Objects.requireNonNull(creditAmount, "creditAmount");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
