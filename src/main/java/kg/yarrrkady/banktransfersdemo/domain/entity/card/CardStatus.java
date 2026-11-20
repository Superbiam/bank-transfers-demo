package kg.yarrrkady.banktransfersdemo.domain.entity.card;

public enum CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED;

    public boolean canTransitionTo(CardStatus targetStatus) {
        return switch (this) {
            case ACTIVE -> targetStatus == BLOCKED;
            case BLOCKED -> targetStatus == ACTIVE;
            case EXPIRED -> false;
        };
    }
} 
