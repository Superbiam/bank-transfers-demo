package kg.yarrrkady.banktransfersdemo.domain.exception.money;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InvalidExchangeRateException extends DomainException {

    public static final String CODE = "INVALID_EXCHANGE_RATE";

    public InvalidExchangeRateException(String message) {
        super(CODE, message);
    }
    
    public InvalidExchangeRateException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
