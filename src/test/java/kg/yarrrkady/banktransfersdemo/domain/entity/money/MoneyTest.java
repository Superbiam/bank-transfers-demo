package kg.yarrrkady.banktransfersdemo.domain.entity.money;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.CurrencyMismatchException;
import kg.yarrrkady.banktransfersdemo.domain.exception.money.InvalidMoneyException;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void shouldNormalizeScaleAndAllowTrailingZerosWithinCurrencyPrecision() {
        Money fromDecimal = new Money(new BigDecimal("10.5"), USD);
        Money fromStringWithTrailingZero = Money.of("10.500", "USD");

        assertThat(fromDecimal.amount()).isEqualByComparingTo("10.50");
        assertThat(fromDecimal.amount().scale()).isEqualTo(2);
        assertThat(fromDecimal.currency()).isEqualTo(USD);
        assertThat(fromStringWithTrailingZero).isEqualTo(fromDecimal);
    }

    @ParameterizedTest 
    @CsvSource ({"USD,2", "EUR,2", "KGS,2", "JPY,0", "KWD,3", "GBP,2"})
    void ofShouldAcceptSupportedCurrencyAndApplyItsScale(String validCode, int expectedScale) {
        Money money = Money.of("10", validCode);

        assertThat(money.amount().scale()).isEqualTo(expectedScale);
        assertThat(money.currency().getCurrencyCode()).isEqualTo(validCode);
    }

    @ParameterizedTest 
    @CsvSource ({"USD,2", "EUR,2", "KGS,2", "JPY,0", "KWD,3", "GBP,2"})
    void ofShouldRejectOneDigitMoreThanCurrencyAllows(String code, int scale) {
        String tooPrecise = "1." + "1".repeat(scale + 1);
        
        assertDomainError(
            () -> Money.of(tooPrecise, code), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-number", "", "   ", "12.34.56", "NaN", "Infinity", 
        "1E+2", "1e2", "1E999999999", "+10.00", ".5", "5.", "1,5", "1 000"})
    void ofShouldRejectInvalidAmountFormat(String invalidAmount) {
        assertDomainError(
            () -> Money.of(invalidAmount, "USD"), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @Test
    void constructorShouldRejectNullCurrency() {
        assertDomainError(
            () -> new Money(new BigDecimal("10.00"), null), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @Test
    void constructorShouldRejectUnsupportedCurrency() {
        assertDomainError(
            () -> new Money(new BigDecimal("10.00"), Currency.getInstance("XAU")), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @Test
    void ofShouldRejectNullCurrencyCode() {
        assertDomainError(
            () -> Money.of("10.00", null), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"XYZ", "XXX", "usd"})
    void shouldRejectUnsupportedOrInvalidCurrency(String invalidCurrencyCode) {
        assertDomainError(
            () -> Money.of("10.00", invalidCurrencyCode), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @Test
    void shouldAddAndSubtractSameCurrencyMoney() {
        Money first = Money.of("100.25", "USD");
        Money second = Money.of("40.75", "USD");

        Money sum = first.add(second);
        Money diff = first.subtract(second);

        assertThat(sum.amount().scale()).isEqualTo(2);
        assertThat(sum.amount()).isEqualByComparingTo("141.00");
        assertThat(diff.amount().scale()).isEqualTo(2);
        assertThat(diff.amount()).isEqualByComparingTo("59.50");
    }

    @Test
    void shouldRejectArithmeticAndComparisonAcrossDifferentCurrencies() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("10.00", "EUR");

        assertDomainError(
            () -> usd.add(eur), 
            CurrencyMismatchException.class, 
            CurrencyMismatchException.CODE
        );
        assertDomainError(
            () -> usd.subtract(eur), 
            CurrencyMismatchException.class, 
            CurrencyMismatchException.CODE
        );
        assertDomainError(
            () -> usd.compareTo(eur), 
            CurrencyMismatchException.class, 
            CurrencyMismatchException.CODE
        );

        assertThat(usd).isNotEqualTo(eur);
    }

    @Test
    void shouldCheckPositiveAndCompareAmountsInSameCurrency() {
        Money positive = Money.of("1.00", "USD");
        Money zero = Money.of("0.00", "USD");
        Money negative = Money.of("-5.00", "USD");

        Money first = new Money(new BigDecimal("10.0"), EUR);
        Money second = Money.of("10.00", "EUR");

        assertThat(positive.isPositive()).isTrue();
        assertThat(zero.isPositive()).isFalse();
        assertThat(negative.isPositive()).isFalse();

        assertThat(positive.compareTo(zero)).isPositive();
        assertThat(zero.compareTo(positive)).isNegative();
        assertThat(first.compareTo(second)).isZero();
        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldRejectNullAmount() {
        assertDomainError(
            () -> new Money(null, USD), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
        assertDomainError(
            () -> Money.of(null, "USD"), 
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @Test
    void subtractShouldAllowNegativeResult() {
        Money first = Money.of("10.00", "USD");
        Money second = Money.of("15.00", "USD");

        Money result = first.subtract(second);

        assertThat(result.amount()).isEqualByComparingTo("-5.00");
        assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    void addShouldRejectNullOperand() {
        Money money = Money.of("10.00", "USD");

        assertDomainError(
            () -> money.add(null),
            InvalidMoneyException.class, 
            InvalidMoneyException.CODE
        );
    }

    @Test
    void subtractShouldRejectNullOperand() {
        Money money = Money.of("10.00", "USD");

        assertDomainError(
            () -> money.subtract(null),
            InvalidMoneyException.class,
            InvalidMoneyException.CODE
        );
    }

    @Test
    void compareToShouldRejectNullOperand() {
        Money money = Money.of("10.00", "USD");

        assertThatThrownBy(() -> money.compareTo(null))
            .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-5.00", "123456789012345678"})
    void ofShouldAcceptPlainDecimalFormat(String amount) {
        assertThat(Money.of(amount, "USD")).isNotNull();
    }

    @ParameterizedTest
    @CsvSource({"USD,0.00", "EUR,0.00", "JPY,0", "KWD,0.000"})
    void zeroShouldHaveScaleOfCurrency(String code, String expected) {
        Money zero = Money.zero(Currency.getInstance(code));

        assertThat(zero.amount()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void zeroShouldEqualParsedZero() {
        assertThat(Money.zero(USD)).isEqualTo(Money.of("0.00", "USD"));
    }

    @Test
    void zeroShouldRejectNullAndUnsupportedCurrency() {
        assertDomainError(() -> Money.zero(null),
                InvalidMoneyException.class, InvalidMoneyException.CODE);
        assertDomainError(() -> Money.zero(Currency.getInstance("XAU")),
                InvalidMoneyException.class, InvalidMoneyException.CODE);
    }

    @Test
    void isZeroAndIsNegativeShouldRespectSign() {
        assertThat(Money.of("0.00", "USD").isZero()).isTrue();
        assertThat(Money.of("0.01", "USD").isZero()).isFalse();
        assertThat(Money.of("-0.01", "USD").isNegative()).isTrue();
        assertThat(Money.of("0.00", "USD").isNegative()).isFalse();
        assertThat(Money.of("1.00", "USD").isNegative()).isFalse();
    }

    private static void assertDomainError(ThrowingCallable call, Class<? extends DomainException> type, String code) {
        assertThatThrownBy(call)
            .isInstanceOfSatisfying(type, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}