package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.nimbusds.jose.shaded.jcip.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.Transaction;
import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.TransactionStatus;

@Entity
@Table(name = "transactions")
@Immutable
public class TransactionEntity extends InsertableEntity {

    @Id
    private UUID id;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal debitAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "debit_currency", nullable = false, length = 3)
    private String debitCurrency;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal creditAmount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "credit_currency", nullable = false, length = 3)
    private String creditCurrency;

    @Column(precision = 19, scale = 8)           // null, если валюта одна
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    private TransactionStatus status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private String comment;

    protected TransactionEntity() {
    }

    public TransactionEntity(Transaction tx) {
        this.id = tx.id();
        this.fromAccountId = tx.fromAccountId();
        this.toAccountId = tx.toAccountId();
        this.debitAmount = tx.debitAmount().amount();
        this.debitCurrency = tx.debitAmount().currency().getCurrencyCode();
        this.creditAmount = tx.creditAmount().amount();
        this.creditCurrency = tx.creditAmount().currency().getCurrencyCode();
        this.rate = tx.rate();
        this.status = tx.status();
        this.idempotencyKey = tx.idempotencyKey();
        this.createdAt = tx.createdAt();
        this.comment = tx.comment();
    }

    static Transaction toDomain(TransactionEntity e) {
        return new Transaction(
            e.getId(), e.getFromAccountId(), e.getToAccountId(),
            new Money(e.getDebitAmount(), Currency.getInstance(e.getDebitCurrency())),
            new Money(e.getCreditAmount(), Currency.getInstance(e.getCreditCurrency())),
            e.getRate(), e.getStatus(), e.getIdempotencyKey(), e.getCreatedAt(), e.getComment());
    }

    public UUID getId() {
        return id;
    }

    public UUID getFromAccountId() {
        return fromAccountId;
    }

    public UUID getToAccountId() {
        return toAccountId;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public String getDebitCurrency() {
        return debitCurrency;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public String getCreditCurrency() {
        return creditCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getComment() {
        return comment;
    }

}
