package kg.yarrrkady.banktransfersdemo.domain.exception.money;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class CurrencyMismatchException extends DomainException {
    
    public static final String CODE = "CURRENCY_MISMATCH";

    public CurrencyMismatchException(String message) {
        super(CODE, message);
    }
}
