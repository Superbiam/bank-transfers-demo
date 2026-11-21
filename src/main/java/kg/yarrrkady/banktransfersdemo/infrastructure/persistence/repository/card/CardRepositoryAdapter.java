package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.repository.card;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import kg.yarrrkady.banktransfersdemo.application.port.card.CardRepository;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.Card;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardNumber;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardStatus;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.CardNotFoundException;
import kg.yarrrkady.banktransfersdemo.domain.exception.card.CardNumberTakenException;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.CardEntity;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.mapper.CardMapper;
import kg.yarrrkady.banktransfersdemo.infrastructure.security.PanProtector;

@Repository
class CardRepositoryAdapter implements CardRepository {

    private final CardJpaRepository jpa;
    private final JdbcClient jdbc;
    private final PanProtector protector;

    CardRepositoryAdapter(CardJpaRepository jpa, JdbcClient jdbc, PanProtector protector) {
        this.jpa = jpa;
        this.jdbc = jdbc;
        this.protector = protector;
    }

    @Override
    public Optional<Card> findById(UUID id) {
        return jpa.findById(id).map(CardMapper::toDomain);
    }

    @Override
    public Card lockById(UUID id) {
        return jpa.findByIdForUpdate(id)
                .map(CardMapper::toDomain)
                .orElseThrow(() -> new CardNotFoundException("Card " + id + " not found"));
    }

    @Override
    public void save(Card card) {
        CardEntity entity = jpa.findById(card.getId()).orElseThrow();   // тот же экземпляр из контекста
        entity.applyStatus(card.getCardStatus());
    }

    @Override
    public boolean existsActiveByAccountId(UUID accountId, LocalDate today) {
        return jpa.existsByAccountIdAndStatusAndExpiresOnGreaterThanEqual(
                accountId, CardStatus.ACTIVE, today);
    }

    @Override
    public void insert(Card card, CardNumber number) {
        try {
            jdbc.sql("""
                        insert into cards
                            (id, account_id, pan_encrypted, pan_hmac,
                            card_bin, card_last4, pan_length,
                            expires_on, card_type, card_status, version)
                        values
                            (:id, :accountId, :encrypted, :hmac,
                            :bin, :last4, :length,
                            :expiresOn, :type, :status, 0)
                    """)
                .param("id", card.getId())
                .param("accountId", card.getAccountId())
                .param("bin", number.bin())
                .param("last4", number.last4())
                .param("length", (short) number.length())
                .param("encrypted", protector.encrypt(card.getId(), number.value()))
                .param("hmac", protector.hmac(number.value()))
                .param("expiresOn", card.getExpiresOn())
                .param("type", card.getCardType().name())
                .param("status", card.getCardStatus().name())
                .update();
        } catch (DuplicateKeyException e) {
            throw new CardNumberTakenException("Card number is already registered");
        }
    }
}
