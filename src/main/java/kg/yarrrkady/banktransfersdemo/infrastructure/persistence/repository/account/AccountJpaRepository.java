package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.account;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.AccountEntity;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id in :ids order by a.id")
    List<AccountEntity> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);
}
