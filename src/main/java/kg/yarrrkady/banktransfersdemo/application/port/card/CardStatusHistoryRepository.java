package kg.yarrrkady.banktransfersdemo.application.port.card;

import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatusChange;

public interface CardStatusHistoryRepository {
    void insert(CardStatusChange change);
}