package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.account;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import kg.yarrrkady.banktransfersdemo.application.port.account.AccountRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;
import kg.yarrrkady.banktransfersdemo.domain.exception.account.AccountNotFoundException;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.AccountEntity;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.mapper.AccountMapper;

@Repository
class AccountRepositoryAdapter implements AccountRepository {

    private final AccountJpaRepository jpa;
    private final JdbcClient jdbc;
    private final EntityManager em;

    AccountRepositoryAdapter(AccountJpaRepository jpa, JdbcClient jdbc, EntityManager em) {
        this.jpa = jpa;
        this.jdbc = jdbc;
        this.em = em;
    }

    @Override
    public List<Account> lockAllByIds(Set<UUID> ids) {
        jdbc.sql("set local lock_timeout = '3s'").update();
        List<AccountEntity> entities = jpa.findAllByIdForUpdate(ids);
        if (entities.size() != ids.size()) {
            throw new AccountNotFoundException("One or more accounts not found");
        }
        return entities.stream().map(AccountMapper::toDomain).toList();
    }

    @Override
    public void save(Account account) {
        AccountEntity entity = jpa.findById(account.getId()).orElseThrow();   // из кэша первого уровня
        entity.applyState(account.getBalance().amount(), account.getStatus());
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpa.findById(id).map(AccountMapper::toDomain);
    }

    @Override
    public void insert(Account account) {
        em.persist(new AccountEntity(account));
    }
}
