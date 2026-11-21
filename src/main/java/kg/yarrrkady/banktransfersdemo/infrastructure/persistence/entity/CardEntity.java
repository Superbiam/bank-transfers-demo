package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatus;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardType;

@Entity
@Table(name = "cards")
public class CardEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "card_bin", nullable = false, length = 6)
    private String cardBin;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "card_last4", nullable = false, length = 4)
    private String cardLast4;

    @Column(name = "pan_length", nullable = false)
    private short panLength;

    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_status", nullable = false)
    private CardStatus status;

    @Version
    private long version;

    protected CardEntity() {
    }

    public void applyStatus(CardStatus status) {
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getCardBin() {
        return cardBin;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public short getPanLength() {
        return panLength;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public CardType getType() {
        return type;
    }

    public CardStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
    
}
