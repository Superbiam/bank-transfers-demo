package kg.yarrrkady.banktransfersdemo.domain.exception.idempotency;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public final class IdempotencyKeyReusedException extends DomainException {

    public static final String CODE = "IDEMPOTENCY_KEY_REUSED";

    public IdempotencyKeyReusedException(String message) {
        super(CODE, message);
    }
}
