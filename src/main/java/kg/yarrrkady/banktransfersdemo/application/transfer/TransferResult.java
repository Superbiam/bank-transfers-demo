package kg.yarrrkady.banktransfersdemo.application.transfer;

import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.Transaction;

public record TransferResult(Transaction transaction, boolean replayed) {

}
