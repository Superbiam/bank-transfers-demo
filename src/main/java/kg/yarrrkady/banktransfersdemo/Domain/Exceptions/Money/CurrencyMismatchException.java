package kg.yarrrkady.banktransfersdemo.Domain.Exceptions.Money;

import kg.yarrrkady.banktransfersdemo.Domain.Exceptions.DomainException;

public class CurrencyMismatchException extends DomainException {
    
    public static final String CODE = "CURRENCY_MISMATCH";

    public CurrencyMismatchException(String message) {
        super(CODE, message);
    }
}
