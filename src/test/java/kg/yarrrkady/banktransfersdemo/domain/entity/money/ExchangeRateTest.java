package kg.yarrrkady.banktransfersdemo.domain.entity.money;

import static org.assertj.core.api.Assertions.assertThat;
import static kg.yarrrkady.banktransfersdemo.support.DomainAssertions.assertDomainError;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidExchangeRateException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

public class ExchangeRateTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Currency KGS = Currency.getInstance("KGS");
    private static final Currency JPY = Currency.getInstance("JPY");

    @ParameterizedTest
    @CsvSource ({
        "10.24,0.1,1.02",
        "10.25,0.1,1.02",
        "10.26,0.1,1.03",
        "10.35,0.1,1.04",
        "10.45,0.1,1.04",
        "10.55,0.1,1.06"
    })
    void convertShouldRoundHalfToEven(String amount, String rate, String expected) {
        ExchangeRate usdToEur = new ExchangeRate(USD, EUR, new BigDecimal(rate));

        Money result = usdToEur.convert(Money.of(amount, "USD"));

        assertThat(result.amount()).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest
    @CsvSource ({
        "EUR,0.92,9.20",
        "KGS,87.5,875.00",
        "JPY,148.5,1485",
        "GBP,0.76,7.60",
        "KWD,0.307,3.070"
    }) 
    void convertShouldReturnAmountInTargetCurrency (String code, String rate, String expected) {
        Currency target = Currency.getInstance(code);
        ExchangeRate usdToTarget = new ExchangeRate(USD, target, new BigDecimal(rate));

        Money result = usdToTarget.convert(Money.of("10.00", "USD"));

        assertThat(result.currency()).isEqualTo(target);
        assertThat(result.amount()).isEqualTo(new BigDecimal(expected));
    }

    @ParameterizedTest
    @ValueSource (strings = {"0","0.00","-1.00","-0.00000001"})
    void constructorShouldRejectNonPositiveRate(String rate) {
        assertDomainError(
            () -> new ExchangeRate(USD, EUR, new BigDecimal(rate)),
            InvalidExchangeRateException.class, 
            InvalidExchangeRateException.CODE
        );
    }

    @ParameterizedTest
    @CsvSource({"148.5,148", "149.5,150"})
    void convertShouldRoundHalfToEvenForZeroFractionCurrency(String rate, String expected) {
        ExchangeRate usdToJpy = new ExchangeRate(USD, JPY, new BigDecimal(rate));

        Money result = usdToJpy.convert(Money.of("1.00", "USD"));

        assertThat(result.amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void convertShouldRejectNullMoney() {
        ExchangeRate usdToEur = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));

        assertDomainError(
            () -> usdToEur.convert(null),
            InvalidMoneyException.class,
            InvalidMoneyException.CODE
        );
    }

    @Test 
    void constructorShouldRejectNullRate() {
        assertDomainError(
            () -> new ExchangeRate(USD, EUR, null),
            InvalidExchangeRateException.class,
            InvalidExchangeRateException.CODE
        );
    }

    @Test
    void constructorShouldRejectRateWithMoreThanEightDecimalPlaces() {
        assertDomainError(
            () -> new ExchangeRate(USD, EUR, new BigDecimal("0.123456789")),
            InvalidExchangeRateException.class,
            InvalidExchangeRateException.CODE
        );
    }

    @Test
    void constructorShouldRejectSameCurrencies() {
        assertDomainError(
            () -> new ExchangeRate(USD, USD, new BigDecimal("1.00")),
            InvalidExchangeRateException.class,
            InvalidExchangeRateException.CODE
        );
    }

    @Test
    void constructorShouldRejectUnsupportedCurrency() {
        assertDomainError(
            () -> new ExchangeRate(USD, Currency.getInstance("XAU"), new BigDecimal("1.00")),
            InvalidMoneyException.class,
            InvalidMoneyException.CODE
        );
    }

    @Test 
    void convertShouldRejectDifferentMoneyCurrency() {
        ExchangeRate usdToEur = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));

        Money diffMoney = Money.of("10.00", "KGS");

        assertDomainError(
            () -> usdToEur.convert(diffMoney), 
            CurrencyMismatchException.class, 
            CurrencyMismatchException.CODE
        );
    }

    @Test 
    void rateShouldBeEqualRegardlessOfTrailingZeros() {
        ExchangeRate a = new ExchangeRate(USD, EUR, new BigDecimal("0.92"));
        ExchangeRate b = new ExchangeRate(USD, EUR, new BigDecimal("0.920"));

        assertThat(a.rate().scale()).isEqualTo(8);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void ratesWithDifferentValueShouldNotBeEqual() {
        assertThat(
            new ExchangeRate(USD, EUR, new BigDecimal("0.92"))
        ).isNotEqualTo(new ExchangeRate(USD, EUR, new BigDecimal("0.93")));
    }

    @Test
    void rateShouldNotBeEqualWithDifferentTargetCurrency() {
        assertThat(new ExchangeRate(USD, EUR, new BigDecimal("0.92")))
            .isNotEqualTo(new ExchangeRate(USD, KGS, new BigDecimal("0.92")));
    }

    @Test
    void rateShouldNotBeEqualWithDifferentSourceCurrency() {
        assertThat(new ExchangeRate(USD, KGS, new BigDecimal("87.5")))
            .isNotEqualTo(new ExchangeRate(EUR, KGS, new BigDecimal("87.5")));
    }
}
