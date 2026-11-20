package kg.yarrrkady.banktransfersdemo.domain.exception.transfer;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class SameAccountTransferException extends DomainException {

    public static final String CODE = "SAME_ACCOUNT_TRANSFER";

    public SameAccountTransferException(String message) {
        super(CODE, message);
    }
    
}
