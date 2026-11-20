package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InvalidCardExpiryException extends DomainException {

    public static final String CODE = "INVALID_CARD_EXPIRY";

    public InvalidCardExpiryException(String message) {
        super(CODE, message);
    }

}
