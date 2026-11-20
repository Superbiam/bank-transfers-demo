package kg.yarrrkady.banktransfersdemo.domain.exception.account;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InsufficientFundsException extends DomainException {

    public static final String CODE = "INSUFFICIENT_FUNDS";

    protected InsufficientFundsException(String message) {
        super(CODE, message);
    }
    
}
