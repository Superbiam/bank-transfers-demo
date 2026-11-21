package kg.yarrrkady.banktransfersdemo.domain.exception.money;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public final class RateUnavailableException extends DomainException {

    public static final String CODE = "RATE_UNAVAILABLE";

    public RateUnavailableException(String message) {
        super(CODE, message);
    }
}
