package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InvalidCardNumberException extends DomainException {

    public static final String CODE = "INVALID_CARD_NUMBER";

    public InvalidCardNumberException(String message) {
        super(CODE, message);
    }
    
}
