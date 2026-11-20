package kg.yarrrkady.banktransfersdemo.domain.exception.money;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class InvalidMoneyException extends DomainException {

    public static final String CODE = "INVALID_MONEY";

    public InvalidMoneyException(String message) {
        super(CODE, message);
    }

    public InvalidMoneyException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
