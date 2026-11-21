package kg.yarrrkady.banktransfersdemo.domain.entity.card;

import static kg.yarrrkady.banktransfersdemo.support.DomainAssertions.assertDomainError;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardNumberException;

public class CardNumberTest {
    @ParameterizedTest
    @ValueSource(strings = {
        "4111111111111111",
        "5555555555554444",
        "378282246310005",
        "4222222222222"
    })
    void shouldAcceptNumbersThatPassLuhn(String pan) {
        assertThat(new CardNumber(pan).value()).isEqualTo(pan);
    }

    @Test
    void shouldRejectNumberThatFailsLuhnByOneDigit() {
        assertDomainError(
            () -> new CardNumber("4111111111111112"),
            InvalidCardNumberException.class, InvalidCardNumberException.CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "   ",
        "abcdefghijklmnop",
        "4111 1111 1111 1111",
        "4111-1111-1111-1111",
        "4111111111111111 "
    })
    void shouldRejectNonDigitInput(String pan) {
        assertDomainError(
            () -> new CardNumber(pan),
            InvalidCardNumberException.class, InvalidCardNumberException.CODE);
    }

    @Test
    void shouldRejectNull() {
        assertDomainError(
            () -> new CardNumber(null),
            InvalidCardNumberException.class, InvalidCardNumberException.CODE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000000000", "00000000000000000000"})
    void shouldRejectLengthOutsideThirteenToNineteenDigits(String pan) {
        assertDomainError(
            () -> new CardNumber(pan),
            InvalidCardNumberException.class, InvalidCardNumberException.CODE);
    }

    @ParameterizedTest
    @CsvSource({
        "4111111111111111, 411111******1111",
        "378282246310005,  378282*****0005",
        "4222222222222,    422222***2222"
    })
    void maskedShouldKeepFirstSixAndLastFourDigits(String pan, String expected) {
        assertThat(new CardNumber(pan).masked()).isEqualTo(expected);
    }

    @Test
    void toStringShouldNeverExposeFullNumber() {
        CardNumber number = new CardNumber("4111111111111111");

        assertThat(number.toString())
            .isEqualTo("411111******1111")
            .doesNotContain("4111111111111111");
    }

    @ParameterizedTest
    @CsvSource({
        "4222222222222,    422222, 2222, 13",
        "378282246310005,  378282, 0005, 15",
        "4111111111111111, 411111, 1111, 16"
    })
    void shouldExposeBinLast4AndLength(String pan, String bin, String last4, int length) {
        CardNumber number = new CardNumber(pan);

        assertThat(number.bin()).isEqualTo(bin);
        assertThat(number.last4()).isEqualTo(last4);
        assertThat(number.length()).isEqualTo(length);
    }

    @ParameterizedTest
    @ValueSource(strings = {"4222222222222", "378282246310005", "4111111111111111"})
    void maskShouldRoundTripFromStoredParts(String pan) {
        CardNumber number = new CardNumber(pan);

        assertThat(CardNumber.mask(number.bin(), number.last4(), number.length()))
            .isEqualTo(number.masked());
    }
}
