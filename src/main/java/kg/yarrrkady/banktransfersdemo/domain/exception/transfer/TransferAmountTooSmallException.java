package kg.yarrrkady.banktransfersdemo.domain.exception.transfer;

import kg.yarrrkady.banktransfersdemo.domain.exception.DomainException;

public class TransferAmountTooSmallException extends DomainException {
    
    public static final String CODE = "TRANSFER_AMOUNT_TOO_SMALL";

    public TransferAmountTooSmallException(String message) {
        super(CODE, message);
    }
}
