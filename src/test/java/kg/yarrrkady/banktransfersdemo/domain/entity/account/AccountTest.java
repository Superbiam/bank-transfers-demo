package kg.yarrrkady.banktransfersdemo.domain.entity.account;

import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountBalanceNotZeroException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountClosedException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountHasActiveCardsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.InsufficientFundsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

import static org.assertj.core.api.Assertions.assertThat;
import static kg.yarrrkady.banktransfersdemo.support.DomainAssertions.assertDomainError;

public class AccountTest {

    private static UUID ID = UUID.randomUUID();
    private static UUID CLIENT_ID = UUID.randomUUID();
    private static String ACCOUNT_NUMBER = "ACC1234567890";
    private static Currency USD = Currency.getInstance("USD");
    
    // ---------- open ----------

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "KWD", "JPY"})
    void openShouldCreateActiveAccountWithZeroBalanceInGivenCurrency(String code) {
        Currency currency = Currency.getInstance(code);

        Account account = Account.open(ID, CLIENT_ID, ACCOUNT_NUMBER, currency);

        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(account.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(account.getBalance().amount()).isZero();
        assertThat(account.getBalance().currency()).isEqualTo(currency);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getVersion()).isZero();
    }

    // ---------- credit ----------

    @Test
    void creditShouldIncreaseBalance() {
        Account account = activeAccountWithBalance("10.00");

        account.credit(Money.of("5.25", "USD"));

        assertThat(account.getBalance()).isEqualTo(Money.of("15.25", "USD"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void creditShouldRejectNonPositiveAmountAndKeepBalance(String amount) {
        Account account = activeAccountWithBalance("10.00");

        assertDomainError(
            () -> account.credit(Money.of(amount, "USD")),
            InvalidMoneyException.class, InvalidMoneyException.CODE);

        assertThat(account.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void creditShouldRejectNullAmount() {
        Account account = activeAccountWithBalance("10.00");

        assertDomainError(
            () -> account.credit(null),
            InvalidMoneyException.class, InvalidMoneyException.CODE);
    }

    @Test
    void creditShouldRejectOtherCurrencyAndKeepBalance() {
        Account account = activeAccountWithBalance("10.00");

        assertDomainError(
            () -> account.credit(Money.of("5.00", "EUR")),
            CurrencyMismatchException.class, CurrencyMismatchException.CODE);

        assertThat(account.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    // ---------- debit ----------

    @Test
    void debitShouldDecreaseBalance() {
        Account account = activeAccountWithBalance("10.00");

        account.debit(Money.of("3.25", "USD"));

        assertThat(account.getBalance()).isEqualTo(Money.of("6.75", "USD"));
    }

    @Test
    void debitShouldAllowWithdrawingEntireBalance() {
        Account account = activeAccountWithBalance("5.00");

        account.debit(Money.of("5.00", "USD"));

        assertThat(account.getBalance()).isEqualTo(Money.zero(USD));
    }

    @Test
    void debitShouldRejectAmountOneCentAboveBalanceAndKeepBalance() {
        Account account = activeAccountWithBalance("5.00");

        assertDomainError(
            () -> account.debit(Money.of("5.01", "USD")),
            InsufficientFundsException.class, InsufficientFundsException.CODE);

        assertThat(account.getBalance()).isEqualTo(Money.of("5.00", "USD"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00"})
    void debitShouldRejectNonPositiveAmountAndKeepBalance(String amount) {
        Account account = activeAccountWithBalance("10.00");

        assertDomainError(
            () -> account.debit(Money.of(amount, "USD")),
            InvalidMoneyException.class, InvalidMoneyException.CODE);

        assertThat(account.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    @Test
    void debitShouldRejectNullAmount() {
        Account account = activeAccountWithBalance("10.00");

        assertDomainError(
            () -> account.debit(null),
            InvalidMoneyException.class, InvalidMoneyException.CODE);
    }

    @Test
    void debitShouldRejectOtherCurrencyAndKeepBalance() {
        Account account = activeAccountWithBalance("10.00");

        assertDomainError(
            () -> account.debit(Money.of("5.00", "EUR")),
            CurrencyMismatchException.class, CurrencyMismatchException.CODE);

        assertThat(account.getBalance()).isEqualTo(Money.of("10.00", "USD"));
    }

    // ---------- closed account ----------

    @Test
    void creditShouldRejectClosedAccount() {
        Account account = closedAccount();

        assertDomainError(
            () -> account.credit(Money.of("1.00", "USD")),
            AccountClosedException.class, AccountClosedException.CODE);
    }

    @Test
    void debitShouldRejectClosedAccount() {
        Account account = closedAccount();

        assertDomainError(
            () -> account.debit(Money.of("1.00", "USD")),
            AccountClosedException.class, AccountClosedException.CODE);
    }

    @Test
    void closedStatusShouldTakePrecedenceOverInvalidAmount() {
        Account account = closedAccount();

        assertDomainError(
            () -> account.credit(Money.of("0.00", "USD")),
            AccountClosedException.class, AccountClosedException.CODE);
    }

    // ---------- close ----------

    @Test
    void closeShouldMarkAccountClosedWhenBalanceIsZeroAndNoActiveCards() {
        Account account = activeAccountWithBalance("0.00");

        account.close(false);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeShouldWorkAfterBalanceWasWithdrawnToZero() {
        Account account = activeAccountWithBalance("5.00");
        account.debit(Money.of("5.00", "USD"));

        account.close(false);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeShouldRejectNonZeroBalanceAndStayActive() {
        Account account = activeAccountWithBalance("0.01");

        assertDomainError(
            () -> account.close(false),
            AccountBalanceNotZeroException.class, AccountBalanceNotZeroException.CODE);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void closeShouldRejectActiveCardsAndStayActive() {
        Account account = activeAccountWithBalance("0.00");

        assertDomainError(
            () -> account.close(true),
            AccountHasActiveCardsException.class, AccountHasActiveCardsException.CODE);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void closeShouldRejectAlreadyClosedAccount() {
        Account account = closedAccount();

        assertDomainError(
            () -> account.close(false),
            AccountClosedException.class, AccountClosedException.CODE);
    }

    // ---------- helpers ----------

    private static Account activeAccountWithBalance(String amount) {
        return Account.restore(ID, CLIENT_ID, ACCOUNT_NUMBER, Money.of(amount, "USD"), AccountStatus.ACTIVE, 1);
    }

    private static Account closedAccount() {
        return Account.restore(ID, CLIENT_ID, ACCOUNT_NUMBER, Money.zero(USD), AccountStatus.CLOSED, 1);
    }
}
