package kg.yarrrkady.banktransfersdemo.domain.entity.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidExchangeRateException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

public record ExchangeRate(Currency from, Currency to, BigDecimal rate) {

    private static final int RATE_SCALE = 8;
    private static final RoundingMode CONVERSION_ROUNDING = RoundingMode.HALF_EVEN;

    public ExchangeRate {
        if (from == null || to == null || rate == null) {
            throw new InvalidExchangeRateException(
                "Currencies and rate must not be null"
            );
        }
        SupportedCurrencies.requireSupported(from);
        SupportedCurrencies.requireSupported(to);
        if (from.equals(to)) {
            throw new InvalidExchangeRateException(
                "Exchange rate requires different currencies"
            );
        }
        if (rate.signum() <= 0) {
            throw new InvalidExchangeRateException(
                "Rate must be positive"
            );
        }
        try {
            rate = rate.setScale(RATE_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidExchangeRateException(
                "Rate has more than " + RATE_SCALE + " decimal places", e
            );
        }
    }
    
    public Money convert(Money source) {
        if (source == null) {
            throw new InvalidMoneyException("Money must not be null");
        }
        if (!source.currency().equals(from)) {
            throw new CurrencyMismatchException(
                "Rate is for %s but money is in %s".formatted(from.getCurrencyCode(), source.currency().getCurrencyCode())
            );
        }
        BigDecimal converted = source.amount()
            .multiply(rate)
            .setScale(to.getDefaultFractionDigits(), CONVERSION_ROUNDING);
        return new Money(converted, to);
    }
}
