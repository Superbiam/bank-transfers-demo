package kg.yarrrkady.banktransfersdemo.domain.exception.account;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class AccountHasActiveCardsException extends DomainException {

    public static final String CODE = "ACCOUNT_HAS_ACTIVE_CARDS";

    public AccountHasActiveCardsException(String message) {
        super(CODE, message);
    }

}
