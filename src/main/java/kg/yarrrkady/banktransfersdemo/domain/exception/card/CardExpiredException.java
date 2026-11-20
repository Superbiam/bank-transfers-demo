package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class CardExpiredException extends DomainException {

    public static final String CODE = "CARD_EXPIRED";

    public CardExpiredException(String message) {
        super(CODE, message);
    }
    
}
