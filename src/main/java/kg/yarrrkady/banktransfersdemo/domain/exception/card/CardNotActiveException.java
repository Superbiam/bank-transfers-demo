package kg.yarrrkady.banktransfersdemo.domain.exception.card;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class CardNotActiveException extends DomainException {
    
    public static final String CODE = "CARD_NOT_ACTIVE";

    public CardNotActiveException(String message) {
        super(CODE, message);
    }

}
