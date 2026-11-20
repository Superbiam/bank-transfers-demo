package kg.yarrrkady.banktransfersdemo.domain.entity.card;

import static kg.yarrrkady.banktransfersdemo.support.DomainAssertions.assertDomainError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.AccountStatus;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountClosedException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.CardExpiredException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.CardNotActiveException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardExpiryException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardStatusChangeException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardStatusTransitionException;

public class CardTest {
    
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final Currency USD = Currency.getInstance("USD");
    private static final UUID CARD_ID = UUID.randomUUID();
    private static final UUID ACCOUNT_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final CardNumber PAN = new CardNumber("4111111111111111");
    private static final LocalDate FAR_FUTURE = LocalDate.of(2030, 12, 31);
    private static final LocalDate YESTERDAY = LocalDate.of(2026, 11, 19);

    // ---------- issue ----------

    @Test
    void issueShouldCreateActiveCardWithMaskedNumberAndEndOfMonthExpiry() {
        Account account = activeAccount();

        Card card = Card.issue(CARD_ID, account, PAN, CardType.DEBIT, YearMonth.of(2030, 10), NOW);

        assertThat(card.getId()).isEqualTo(CARD_ID);
        assertThat(card.getAccountId()).isEqualTo(account.getId());
        assertThat(card.getMaskedNumber()).isEqualTo("411111******1111");
        assertThat(card.getExpiresOn()).isEqualTo(LocalDate.of(2030, 10, 31));
        assertThat(card.getCardType()).isEqualTo(CardType.DEBIT);
        assertThat(card.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.getVersion()).isZero();
    }

    @Test
    void issueShouldRejectClosedAccount() {
        Account closed = Account.restore(
            ACCOUNT_ID, CLIENT_ID, "ACC1234567890", Money.zero(USD), AccountStatus.CLOSED, 1);

        assertDomainError(
            () -> Card.issue(CARD_ID, closed, PAN, CardType.DEBIT, YearMonth.of(2030, 10), NOW),
            AccountClosedException.class, AccountClosedException.CODE);
    }

    @Test
    void issueShouldAcceptExpiryInCurrentMonth() {
        Card card = Card.issue(CARD_ID, activeAccount(), PAN, CardType.DEBIT, YearMonth.of(2026, 11), NOW);

        assertThat(card.getExpiresOn()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void issueShouldRejectExpiryInPreviousMonth() {
        assertDomainError(
            () -> Card.issue(CARD_ID, activeAccount(), PAN, CardType.DEBIT, YearMonth.of(2026, 10), NOW),
            InvalidCardExpiryException.class, InvalidCardExpiryException.CODE);
    }

    @Test
    void issueShouldAcceptLastMonthWithinTenYears() {
        Card card = Card.issue(CARD_ID, activeAccount(), PAN, CardType.DEBIT, YearMonth.of(2036, 10), NOW);

        assertThat(card.getExpiresOn()).isEqualTo(LocalDate.of(2036, 10, 31));
    }

    @Test
    void issueShouldRejectExpiryBeyondTenYears() {
        assertDomainError(
            () -> Card.issue(CARD_ID, activeAccount(), PAN, CardType.DEBIT, YearMonth.of(2036, 11), NOW),
            InvalidCardExpiryException.class, InvalidCardExpiryException.CODE);
    }

    // ---------- changeStatus ----------

    @Test
    void changeStatusShouldBlockActiveCardAndReturnAuditRecord() {
        Card card = card(CardStatus.ACTIVE, FAR_FUTURE);

        CardStatusChange change = card.changeStatus(CardStatus.BLOCKED, "operator1", "lost card", NOW);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(change.cardId()).isEqualTo(CARD_ID);
        assertThat(change.from()).isEqualTo(CardStatus.ACTIVE);
        assertThat(change.to()).isEqualTo(CardStatus.BLOCKED);
        assertThat(change.actor()).isEqualTo("operator1");
        assertThat(change.reason()).isEqualTo("lost card");
        assertThat(change.changedAt()).isEqualTo(NOW);
    }

    @Test
    void changeStatusShouldUnblockBlockedCard() {
        Card card = card(CardStatus.BLOCKED, FAR_FUTURE);

        CardStatusChange change = card.changeStatus(CardStatus.ACTIVE, "operator1", "found", NOW);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(change.from()).isEqualTo(CardStatus.BLOCKED);
        assertThat(change.to()).isEqualTo(CardStatus.ACTIVE);
    }

    @ParameterizedTest
    @CsvSource({
        "ACTIVE,  ACTIVE",
        "ACTIVE,  EXPIRED",
        "BLOCKED, BLOCKED",
        "BLOCKED, EXPIRED"
    })
    void changeStatusShouldRejectTransitionOutsideTableAndKeepStatus(CardStatus current, CardStatus target) {
        Card card = card(current, FAR_FUTURE);

        assertDomainError(
            () -> card.changeStatus(target, "operator1", "reason", NOW),
            InvalidCardStatusTransitionException.class, InvalidCardStatusTransitionException.CODE);

        assertThat(card.getCardStatus()).isEqualTo(current);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void changeStatusShouldRejectBlankReasonAndKeepStatus(String reason) {
        Card card = card(CardStatus.ACTIVE, FAR_FUTURE);

        assertDomainError(
            () -> card.changeStatus(CardStatus.BLOCKED, "operator1", reason, NOW),
            InvalidCardStatusChangeException.class, InvalidCardStatusChangeException.CODE);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void changeStatusShouldRejectNullReasonAndKeepStatus() {
        Card card = card(CardStatus.ACTIVE, FAR_FUTURE);

        assertDomainError(
            () -> card.changeStatus(CardStatus.BLOCKED, "operator1", null, NOW),
            InvalidCardStatusChangeException.class, InvalidCardStatusChangeException.CODE);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void changeStatusShouldRejectNullTargetAndNullActor() {
        Card card = card(CardStatus.ACTIVE, FAR_FUTURE);

        assertThatThrownBy(() -> card.changeStatus(null, "operator1", "reason", NOW))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> card.changeStatus(CardStatus.BLOCKED, null, "reason", NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void changeStatusShouldNotUnblockCardExpiredByDate() {
        Card card = card(CardStatus.BLOCKED, YESTERDAY);

        assertDomainError(
            () -> card.changeStatus(CardStatus.ACTIVE, "operator1", "found", NOW),
            CardExpiredException.class, CardExpiredException.CODE);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void changeStatusShouldReportExpiredBeforeInvalidTransitionForCardExpiredByDate() {
        Card card = card(CardStatus.BLOCKED, YESTERDAY);

        assertDomainError(
            () -> card.changeStatus(CardStatus.BLOCKED, "operator1", "reason", NOW),
            CardExpiredException.class, CardExpiredException.CODE);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "BLOCKED"})
    void changeStatusShouldRejectAnyTargetForCardStoredAsExpired(CardStatus target) {
        Card card = card(CardStatus.EXPIRED, FAR_FUTURE);

        assertDomainError(
            () -> card.changeStatus(target, "operator1", "reason", NOW),
            CardExpiredException.class, CardExpiredException.CODE);

        assertThat(card.getCardStatus()).isEqualTo(CardStatus.EXPIRED);
    }

    // ---------- isExpired ----------

    @Test
    void cardShouldBeAliveThroughLastSecondOfExpiryDayAndExpiredAfterIt() {
        Card card = card(CardStatus.ACTIVE, LocalDate.of(2033, 10, 31));

        assertThat(card.isExpired(Instant.parse("2033-10-31T23:59:59Z"))).isFalse();
        assertThat(card.isExpired(Instant.parse("2033-11-01T00:00:00Z"))).isTrue();
    }

    @Test
    void cardStoredAsExpiredShouldBeExpiredEvenBeforeExpiryDate() {
        assertThat(card(CardStatus.EXPIRED, FAR_FUTURE).isExpired(NOW)).isTrue();
    }

    // ---------- requireUsable ----------

    @Test
    void requireUsableShouldPassForActiveCard() {
        Card card = card(CardStatus.ACTIVE, FAR_FUTURE);

        assertThatCode(() -> card.requireUsable(NOW)).doesNotThrowAnyException();
    }

    @Test
    void requireUsableShouldRejectBlockedCard() {
        Card card = card(CardStatus.BLOCKED, FAR_FUTURE);

        assertDomainError(
            () -> card.requireUsable(NOW),
            CardNotActiveException.class, CardNotActiveException.CODE);
    }

    @Test
    void requireUsableShouldRejectCardExpiredByDate() {
        Card card = card(CardStatus.ACTIVE, YESTERDAY);

        assertDomainError(
            () -> card.requireUsable(NOW),
            CardExpiredException.class, CardExpiredException.CODE);
    }

    @Test
    void requireUsableShouldReportExpiredBeforeBlockedWhenBoth() {
        Card card = card(CardStatus.BLOCKED, YESTERDAY);

        assertDomainError(
            () -> card.requireUsable(NOW),
            CardExpiredException.class, CardExpiredException.CODE);
    }

    // ---------- helpers ----------

    private static Account activeAccount() {
        return Account.open(ACCOUNT_ID, CLIENT_ID, "ACC1234567890", USD);
    }

    private static Card card(CardStatus status, LocalDate expiresOn) {
        return Card.restore(CARD_ID, ACCOUNT_ID, "411111******1111", expiresOn, CardType.DEBIT, status, 1);
    }
}
