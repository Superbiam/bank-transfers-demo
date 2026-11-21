package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.transfer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.TransactionEntity;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

}
