package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.card;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatus;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.CardEntity;

public interface CardJpaRepository extends JpaRepository<CardEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CardEntity c where c.id = :id")
    Optional<CardEntity> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByAccountIdAndStatusAndExpiresOnGreaterThanEqual(
            UUID accountId, CardStatus status, LocalDate today);
}
