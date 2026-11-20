package kg.yarrrkady.banktransfersdemo.domain.exception.account;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class AccountClosedException extends DomainException {

    public static final String CODE = "ACCOUNT_CLOSED";

    public AccountClosedException( String message) {
        super(CODE, message);
    }
    
}
