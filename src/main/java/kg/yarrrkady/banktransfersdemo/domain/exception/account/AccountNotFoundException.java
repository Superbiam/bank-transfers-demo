package kg.yarrrkady.banktransfersdemo.domain.exception.account;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class AccountNotFoundException extends DomainException {

    public static final String CODE = "ACCOUNT_NOT_FOUND";

    public AccountNotFoundException(String message) {
        super(CODE, message);
    }
}
