package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.transfer;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import kg.yarrrkady.banktransfersdemo.application.port.transfer.TransactionRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.Transaction;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.TransactionEntity;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.mapper.TransactionMapper;

@Repository
class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpa;
    private final EntityManager em;

    TransactionRepositoryAdapter(TransactionJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public void insert(Transaction tx) {
        em.persist(new TransactionEntity(tx));
        em.flush();   // нужен до attachTransaction: внешний ключ idempotency_keys -> transactions
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpa.findById(id).map(TransactionMapper::toDomain);
    }
}
