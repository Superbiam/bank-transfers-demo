package kg.yarrrkady.banktransfersdemo.domain.entity.card;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CardStatusTest {
    
    @ParameterizedTest
    @CsvSource({
        "ACTIVE,  ACTIVE,  false",
        "ACTIVE,  BLOCKED, true",
        "ACTIVE,  EXPIRED, false",
        "BLOCKED, ACTIVE,  true",
        "BLOCKED, BLOCKED, false",
        "BLOCKED, EXPIRED, false",
        "EXPIRED, ACTIVE,  false",
        "EXPIRED, BLOCKED, false",
        "EXPIRED, EXPIRED, false"
    })
    void shouldAllowOnlyActiveBlockedSwap(CardStatus from, CardStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }
}
