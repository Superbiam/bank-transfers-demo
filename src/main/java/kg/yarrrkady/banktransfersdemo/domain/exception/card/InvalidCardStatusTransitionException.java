package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InvalidCardStatusTransitionException extends DomainException {
    
    public static final String CODE = "INVALID_CARD_STATUS_TRANSITION";

    public InvalidCardStatusTransitionException(String message) {
        super(CODE, message);
    }
}
