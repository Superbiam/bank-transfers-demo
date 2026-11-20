package kg.yarrrkady.banktransfersdemo.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public final class DomainAssertions {
    
    private DomainAssertions() {

    }

    public static void assertDomainError(
            ThrowingCallable call, Class<? extends DomainException> type, String code) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(type, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}