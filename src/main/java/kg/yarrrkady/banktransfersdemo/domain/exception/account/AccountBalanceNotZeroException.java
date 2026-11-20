package kg.yarrrkady.banktransfersdemo.domain.exception.account;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class AccountBalanceNotZeroException extends DomainException {

    public static final String CODE = "ACCOUNT_BALANCE_NOT_ZERO";

    public AccountBalanceNotZeroException(String message) {
        super(CODE, message);
    }
    
}
