package kg.yarrrkady.banktransfersdemo.domain.entity.account;

import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountBalanceNotZeroException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountClosedException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountHasActiveCardsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.InsufficientFundsException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

public class Account {

    private final UUID id;
    private final UUID clientId;
    private final String accountNumber;
    private Money balance;
    private AccountStatus status;
    private final long version;
    
    private Account(UUID id, UUID clientId, String accountNumber, 
                   Money balance, AccountStatus status, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.status = Objects.requireNonNull(status, "status");
        this.version = version;
    }

    public static Account open(UUID id, UUID clientId, String accountNumber, Currency currency) {
        return new Account(
            id, clientId, accountNumber, Money.zero(currency), AccountStatus.ACTIVE, 0
        );
    }

    public static Account restore(UUID id, UUID clientId, String accountNumber,
                                    Money balance, AccountStatus status, long version) {
        return new Account(id, clientId, accountNumber, balance, status, version);
    }

    public void credit(Money amount) {
        requireActive();
        requirePositive(amount);
        balance = balance.add(amount);
    }

    public void debit(Money amount) {
        requireActive();
        requirePositive(amount);
        Money newBalance = balance.subtract(amount);
        if (newBalance.isNegative()) {
            throw new InsufficientFundsException(
                "Insufficient funds on account " + accountNumber
            );
        }
        balance = newBalance;
    }

    public void close(boolean hasActiveCards) {
        requireActive();
        if (!balance.isZero()) {
            throw new AccountBalanceNotZeroException(
                "Account balance must be zero to close"
            );
        }
        if (hasActiveCards) {
            throw new AccountHasActiveCardsException("Account has active cards");
        }
        status = AccountStatus.CLOSED;
    }

    private void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new AccountClosedException("Account " + accountNumber + " is closed");
        }
    }

    private static void requirePositive(Money amount) {
        if (amount == null || !amount.isPositive()) {
            throw new InvalidMoneyException("Amount must be positive");
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public Money getBalance() {
        return balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

}