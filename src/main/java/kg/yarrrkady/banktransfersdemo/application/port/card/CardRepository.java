package kg.yarrrkady.banktransfersdemo.application.port.card;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import kg.yarrrkady.banktransfersdemo.domain.entity.card.Card;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardNumber;

public interface CardRepository {
    Optional<Card> findById(UUID id);
    Card lockById(UUID id);
    void save(Card card);
    boolean existsActiveByAccountId(UUID accountId, LocalDate today);
    void insert(Card card, CardNumber number);
}
