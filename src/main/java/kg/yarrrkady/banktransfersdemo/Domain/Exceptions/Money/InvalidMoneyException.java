package kg.yarrrkady.banktransfersdemo.Domain.Exceptions.Money;

import kg.yarrrkady.banktransfersdemo.Domain.Exceptions.DomainException;

public class InvalidMoneyException extends DomainException {

    public static final String CODE = "INVALID_MONEY";

    public InvalidMoneyException(String message) {
        super(CODE, message);
    }

    public InvalidMoneyException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
