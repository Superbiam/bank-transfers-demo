package kg.yarrrkady.banktransfersdemo.domain.entity.transfer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.ExchangeRate;
import kg.yarrrkady.banktransfersdemo.domain.entity.money.Money;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidExchangeRateException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;
import kg.yarrrkady.banktransfersdemo.domain.exception.transfer.SameAccountTransferException;
import kg.yarrrkady.banktransfersdemo.domain.exception.transfer.TransferAmountTooSmallException;

public class TransferDomainService {

    public Transaction execute(UUID transactionId, Account from, Account to, Money amount,
                    ExchangeRate rate, String idempotencyKey, String comment, Instant now) {
        Objects.requireNonNull(transactionId, "transactionId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(now, "now");

        if (!amount.isPositive()) {
            throw new InvalidMoneyException("Amount must be positive");
        }

        if (from.getId().equals(to.getId())) {
            throw new SameAccountTransferException(
                "Cannot transfer to the same account"
            );
        }
        to.requireActive();
        if (!amount.currency().equals(from.getCurrency())) {
            throw new CurrencyMismatchException(
                "Amount currency must match the sender account currency"
            );
        }

        Money credited = creditedAmount(from, to, amount, rate);
        if (!credited.isPositive()) {
            throw new TransferAmountTooSmallException("Credited amount rounds to zero");
        }

        from.debit(amount);
        to.credit(credited);

        return new Transaction(transactionId, from.getId(), to.getId(), amount, credited,
            rate == null ? null : rate.rate(), TransactionStatus.COMPLETED,
            idempotencyKey, now, comment);
    }

    private static Money creditedAmount(Account from, Account to, Money amount, ExchangeRate rate) {
        if (from.getCurrency().equals(to.getCurrency())) {
            if (rate != null) {
                throw new InvalidExchangeRateException(
                    "Rate must not be given for same-currency transfer"
                );
            }
            return amount;
        }
        if (rate == null) {
            throw new InvalidExchangeRateException(
                "Rate is required for cross-currency transfer"
            );
        }
        if (!rate.from().equals(from.getCurrency()) || !rate.to().equals(to.getCurrency())) {
            throw new InvalidExchangeRateException(
                "Rate pair does not match the accounts"
            );
        }
        return rate.convert(amount);
    }
}
