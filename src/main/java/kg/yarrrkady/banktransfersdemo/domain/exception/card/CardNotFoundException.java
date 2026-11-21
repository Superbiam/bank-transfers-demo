package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public final class CardNotFoundException extends DomainException {

    public static final String CODE = "CARD_NOT_FOUND";

    public CardNotFoundException(String message) {
        super(CODE, message);
    }
}
