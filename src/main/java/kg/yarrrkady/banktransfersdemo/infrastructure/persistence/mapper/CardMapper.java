package kg.yarrrkady.banktransfersdemo.infrastructure.persistence.mapper;

import kg.yarrrkady.banktransfersdemo.domain.entity.card.Card;
import kg.yarrrkady.banktransfersdemo.domain.entity.card.CardNumber;
import kg.yarrrkady.banktransfersdemo.infrastructure.persistence.entity.CardEntity;

public class CardMapper {
    public static Card toDomain(CardEntity e) {
        return Card.restore(
            e.getId(), e.getAccountId(),
            CardNumber.mask(e.getCardBin(), e.getCardLast4(), e.getPanLength()),
            e.getExpiresOn(), e.getType(), e.getStatus(), e.getVersion());
    }
}
