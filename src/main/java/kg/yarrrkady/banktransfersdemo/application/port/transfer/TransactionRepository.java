package kg.yarrrkady.banktransfersdemo.application.port.transfer;

import java.util.Optional;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.Transaction;


public interface TransactionRepository {
    void insert(Transaction tx);
    Optional<Transaction> findById(UUID id);
}
