package kg.yarrrkady.banktransfersdemo.domain.entity.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {
    public Money {
        if (amount == null) {
            throw new InvalidMoneyException("Amount must not be null");
        }
        SupportedCurrencies.requireSupported(currency);
        try {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidMoneyException(
                "Too many decimal places for: " + currency.getCurrencyCode(), e
            );
        }
    }

    public static Money of(String amount, String currencyCode) {
        if (amount == null) {
            throw new InvalidMoneyException("Amount must not be null");
        }
        Currency currency = SupportedCurrencies.require(currencyCode);
        BigDecimal decimalAmount;
        try {
            decimalAmount = new BigDecimal(amount);
        } catch (NumberFormatException e) {
            throw new InvalidMoneyException(
                "Invalid amount format: " + amount, e
            );
        }
        return new Money(decimalAmount, currency);
    }

    public Money add(Money other) {
        validateCurrencyMatch(other, "add");
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateCurrencyMatch(other, "subtract");
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    @Override 
    public int compareTo(Money other) {
        validateCurrencyMatch(other, "compare");
        return this.amount.compareTo(other.amount);
    }
    
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private void validateCurrencyMatch(Money other, String operation) {
        if (!this.currency.equals(other.currency)) {
            throw new CurrencyMismatchException(
                "Cannot " + operation + " amounts with different currencies: " + 
                this.currency.getCurrencyCode() + " and " + other.currency.getCurrencyCode()
            );
        }
    }
}
