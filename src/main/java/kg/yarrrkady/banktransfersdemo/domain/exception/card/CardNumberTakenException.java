package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public final class CardNumberTakenException extends DomainException {

    public static final String CODE = "CARD_NUMBER_TAKEN";

    public CardNumberTakenException(String message) {
        super(CODE, message);
    }
}
