package kg.yarrrkady.banktransfersdemo.domain.entity.card;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.AccountStatus;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountClosedException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.CardExpiredException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.CardNotActiveException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardExpiryException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardStatusChangeException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardStatusTransitionException;

public class Card {
    
    private static final int MAX_VALIDITY_YEARS = 10;

    private final UUID id;
    private final UUID accountId;
    private final String maskedNumber;
    private final LocalDate expiresOn;
    private final CardType cardType;
    private CardStatus cardStatus;
    private final long version;

    private Card(UUID id, UUID accountId, String maskedNumber, LocalDate expiresOn, 
                CardType cardType, CardStatus cardStatus, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.maskedNumber = Objects.requireNonNull(maskedNumber, "maskedNumber");
        this.expiresOn = Objects.requireNonNull(expiresOn, "expiresOn");
        this.cardType = Objects.requireNonNull(cardType, "cardType");
        this.cardStatus = Objects.requireNonNull(cardStatus, "cardStatus");
        this.version = version;
    }

    public static Card restore(UUID id, UUID accountId, String maskedNumber, LocalDate expiresOn, 
                CardType cardType, CardStatus cardStatus, long version) {
        return new Card(id, accountId, maskedNumber, expiresOn, cardType, cardStatus, version);
    }

    public static Card issue(UUID id, Account account, CardNumber cardNumber, 
                            CardType cardType, YearMonth expiry, Instant now) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountClosedException("Cannot issue a card for a closed account");
        }
        LocalDate today = today(now);
        LocalDate expiresOn = expiry.atEndOfMonth();
        if (expiresOn.isBefore(today)) {
            throw new InvalidCardExpiryException("Card expiry is in the past");
        }
        if (expiresOn.isAfter(today.plusYears(MAX_VALIDITY_YEARS))) {
            throw new InvalidCardExpiryException("Card expiry is too far in the future");
        }
        return new Card(
            id, account.getId(), cardNumber.masked(), expiresOn, cardType, CardStatus.ACTIVE, 0
        );
    }

    public CardStatusChange changeStatus(CardStatus target, String actor, String reason, Instant now) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(actor, "actor");
        if (reason == null || reason.isBlank()) {
            throw new InvalidCardStatusChangeException("Status change reason must not be blank");
        }
        if (isExpired(now)) {
            throw new CardExpiredException("Card " + id + " is expired");
        }
        if (!cardStatus.canTransitionTo(target)) {
            throw new InvalidCardStatusTransitionException(
                "Cannot change card status from %s to %s".formatted(cardStatus, target)
            );
        }
        CardStatus previous = cardStatus;
        cardStatus = target;
        return new CardStatusChange(id, previous, target, actor, reason, now);
    }

    public void requireUsable(Instant now) {
        if (isExpired(now)) {
            throw new CardExpiredException("Card " + id + " is expired");
        }
        if (cardStatus != CardStatus.ACTIVE) {
            throw new CardNotActiveException("Card " + id + " is not active");
        }
    }

    public boolean isExpired(Instant now) {
        return cardStatus == CardStatus.EXPIRED || today(now).isAfter(expiresOn);
    }

    private static LocalDate today(Instant now) {
        return LocalDate.ofInstant(now, ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public CardType getCardType() {
        return cardType;
    }

    public CardStatus getCardStatus() {
        return cardStatus;
    }

    public long getVersion() {
        return version;
    }

}
