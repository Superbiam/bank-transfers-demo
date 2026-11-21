package kg.yarrrkady.banktransfersdemo.application.port.account;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.account.Account;

public interface AccountRepository {
    Optional<Account> findById(UUID id);
    List<Account> lockAllByIds(Set<UUID> ids);
    void save(Account account);
    void insert(Account account);
}
