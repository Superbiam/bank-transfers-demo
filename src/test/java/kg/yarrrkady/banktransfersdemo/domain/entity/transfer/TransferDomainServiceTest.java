package kg.yarrrkady.banktransfersdemo.domain.entity.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.AccountStatus;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.ExchangeRate;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountClosedException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.InsufficientFundsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidExchangeRateException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;
import kg.yarrrkady.banktransfersdemo.domain.exception.transfer.SameAccountTransferException;
import kg.yarrrkady.banktransfersdemo.domain.exception.transfer.TransferAmountTooSmallException;
import kg.yarrrkady.banktransfersdemo.support.DomainAssertions;

class TransferDomainServiceTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency JPY = Currency.getInstance("JPY");

    private static final UUID TX_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_A = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CLIENT_B = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ACC_1_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACC_2_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ACC_3_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final String IDEMPOTENCY_KEY = "idem-key-001";
    private static final String COMMENT = "monthly settlement";

    private final TransferDomainService service = new TransferDomainService();

    @Test
    void shouldMoveMoneyBetweenSameCurrencyAccountsAndKeepTotalSum() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "20.00", "USD");
        Money totalBefore = from.getBalance().add(to.getBalance());

        service.execute(TX_ID, from, to, Money.of("30.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW);

        assertThat(from.getBalance()).isEqualTo(Money.of("70.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(from.getBalance().add(to.getBalance())).isEqualTo(totalBefore);
    }

    @Test
    void shouldCreateCompletedTransactionWithoutRateForSameCurrencyTransfer() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "20.00", "USD");
        Money amount = Money.of("30.00", "USD");

        Transaction tx = service.execute(TX_ID, from, to, amount, null, IDEMPOTENCY_KEY, COMMENT, NOW);

        assertThat(tx.id()).isEqualTo(TX_ID);
        assertThat(tx.fromAccountId()).isEqualTo(ACC_1_ID);
        assertThat(tx.toAccountId()).isEqualTo(ACC_2_ID);
        assertThat(tx.debitAmount()).isEqualTo(Money.of("30.00", "USD"));
        assertThat(tx.creditAmount()).isEqualTo(Money.of("30.00", "USD"));
        assertThat(tx.debitAmount()).isEqualTo(tx.creditAmount());
        assertThat(tx.rate()).isNull();
        assertThat(tx.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(tx.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(tx.createdAt()).isEqualTo(NOW);
        assertThat(tx.comment()).isEqualTo(COMMENT);
    }

    @Test
    void shouldAllowNullComment() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "20.00", "USD");

        Transaction tx = service.execute(TX_ID, from, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, null, NOW);

        assertThat(tx.comment()).isNull();
    }

    @Test
    void shouldConvertAmountAndStoreRateWithScaleEightForCrossCurrencyTransfer() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "0.00", "EUR");
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));

        Transaction tx = service.execute(TX_ID, from, to, Money.of("10.00", "USD"), rate, IDEMPOTENCY_KEY, COMMENT, NOW);

        assertThat(from.getBalance()).isEqualTo(Money.of("90.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("9.20", "EUR"));
        assertThat(tx.debitAmount()).isEqualTo(Money.of("10.00", "USD"));
        assertThat(tx.creditAmount()).isEqualTo(Money.of("9.20", "EUR"));
        assertThat(tx.rate()).isEqualTo(new BigDecimal("0.92000000"));
        assertThat(tx.status()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldRoundConvertedAmountHalfEven() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "0.00", "EUR");
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.1"));

        Transaction tx = service.execute(TX_ID, from, to, Money.of("10.25", "USD"), rate, IDEMPOTENCY_KEY, COMMENT, NOW);

        assertThat(from.getBalance()).isEqualTo(Money.of("39.75", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("1.02", "EUR"));
        assertThat(tx.creditAmount()).isEqualTo(Money.of("1.02", "EUR"));
    }

    @Test
    void shouldRejectTransferWhenConvertedAmountRoundsToZero() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "10.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "500", "JPY");
        ExchangeRate rate = new ExchangeRate(USD, JPY, new BigDecimal("0.1"));

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("0.01", "USD"), rate, IDEMPOTENCY_KEY, COMMENT, NOW),
            TransferAmountTooSmallException.class,
            TransferAmountTooSmallException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("10.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("500", "JPY"));
    }

    @Test
    void shouldRejectZeroAmount() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("0.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            InvalidMoneyException.class,
            InvalidMoneyException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void shouldRejectNegativeAmount() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("-1.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            InvalidMoneyException.class,
            InvalidMoneyException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void shouldRejectTransferToSameAccount() {
        Account same = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, same, same, Money.of("25.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            SameAccountTransferException.class,
            SameAccountTransferException.CODE
        );

        assertThat(same.getBalance()).isEqualTo(Money.of("100.00", "USD"));
    }

    @Test
    void shouldRejectClosedSenderAccount() {
        Account from = closedAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "20.00", "USD");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            AccountClosedException.class,
            AccountClosedException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("100.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("20.00", "USD"));
    }

    @Test
    void shouldRejectClosedRecipientAccountWithoutDebitingSender() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "100.00", "USD");
        Account to = closedAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "20.00", "USD");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            AccountClosedException.class,
            AccountClosedException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("100.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("20.00", "USD"));
    }

    @Test
    void shouldRejectAmountAboveSenderBalance() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "5.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("5.01", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            InsufficientFundsException.class,
            InsufficientFundsException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("5.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void shouldAllowTransferOfFullBalanceAndLeaveZero() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "5.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        service.execute(TX_ID, from, to, Money.of("5.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW);

        assertThat(from.getBalance()).isEqualTo(Money.of("0.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("15.00", "USD"));
    }

    @Test
    void shouldRejectAmountInCurrencyDifferentFromSender() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "EUR");
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("10.00", "EUR"), rate, IDEMPOTENCY_KEY, COMMENT, NOW),
            CurrencyMismatchException.class,
            CurrencyMismatchException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "EUR"));
    }

    @Test
    void shouldRejectCrossCurrencyTransferWithoutRate() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "EUR");

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW),
            InvalidExchangeRateException.class,
            InvalidExchangeRateException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "EUR"));
    }

    @Test
    void shouldRejectRateForSameCurrencyTransfer() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_3_ID, CLIENT_B, "ACC0000000003", "10.00", "USD");
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), rate, IDEMPOTENCY_KEY, COMMENT, NOW),
            InvalidExchangeRateException.class,
            InvalidExchangeRateException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void shouldRejectRateWithMismatchedCurrencyPair() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "EUR");
        ExchangeRate wrongPair = new ExchangeRate(EUR, USD, new BigDecimal("1.10"));

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), wrongPair, IDEMPOTENCY_KEY, COMMENT, NOW),
            InvalidExchangeRateException.class,
            InvalidExchangeRateException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "EUR"));
    }

    @Test
    void shouldRejectNullTransactionId() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        assertThatThrownBy(() -> service.execute(null, from, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullSenderAccount() {
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        assertThatThrownBy(() -> service.execute(TX_ID, null, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullRecipientAccount() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");

        assertThatThrownBy(() -> service.execute(TX_ID, from, null, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullAmount() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        assertThatThrownBy(() -> service.execute(TX_ID, from, to, null, null, IDEMPOTENCY_KEY, COMMENT, NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullIdempotencyKey() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        assertThatThrownBy(() -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), null, null, COMMENT, NOW))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullTimestamp() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "USD");

        assertThatThrownBy(() -> service.execute(TX_ID, from, to, Money.of("10.00", "USD"), null, IDEMPOTENCY_KEY, COMMENT, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNegativeAmountInCrossCurrencyTransfer() {
        Account from = activeAccount(ACC_1_ID, CLIENT_A, "ACC0000000001", "50.00", "USD");
        Account to = activeAccount(ACC_2_ID, CLIENT_B, "ACC0000000002", "10.00", "EUR");
        ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));

        DomainAssertions.assertDomainError(
            () -> service.execute(TX_ID, from, to, Money.of("-1.00", "USD"), rate, IDEMPOTENCY_KEY, COMMENT, NOW),
            InvalidMoneyException.class,
            InvalidMoneyException.CODE
        );

        assertThat(from.getBalance()).isEqualTo(Money.of("50.00", "USD"));
        assertThat(to.getBalance()).isEqualTo(Money.of("10.00", "EUR"));
    }

    private static Account activeAccount(UUID id, UUID clientId, String number, String amount, String currency) {
        return Account.restore(
            id, clientId, number, Money.of(amount, currency), AccountStatus.ACTIVE, 1
        );
    }

    private static Account closedAccount(UUID id, UUID clientId, String number, String amount, String currency) {
        return Account.restore(
            id, clientId, number, Money.of(amount, currency), AccountStatus.CLOSED, 1
        );
    }
}
