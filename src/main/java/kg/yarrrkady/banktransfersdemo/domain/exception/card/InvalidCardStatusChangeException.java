package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InvalidCardStatusChangeException extends DomainException {

    public static final String CODE = "INVALID_CARD_STATUS_CHANGE";

    public InvalidCardStatusChangeException(String message) {
        super(CODE, message);
    }
    
}
