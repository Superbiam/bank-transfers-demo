package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.card;

import java.sql.Timestamp;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import kg.yarrrkady.banktransfersdemo.application.port.card.CardStatusHistoryRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatusChange;

@Repository
class CardStatusHistoryRepositoryAdapter implements CardStatusHistoryRepository {

    private final JdbcClient jdbc;

    CardStatusHistoryRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void insert(CardStatusChange c) {
        jdbc.sql("""
                insert into card_status_history
                    (card_id, from_status, to_status, changed_by, reason, changed_at)
                values (:cardId, :fromStatus, :toStatus, :changedBy, :reason, :changedAt)
                """)
            .param("cardId", c.cardId())
            .param("fromStatus", c.from().name())
            .param("toStatus", c.to().name())
            .param("changedBy", c.actor())
            .param("reason", c.reason())
            .param("changedAt", Timestamp.from(c.changedAt()))
            .update();
    }
}
