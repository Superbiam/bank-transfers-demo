package kg.yarrrkady.banktransfersdemo.domain.entity.card;

import java.util.regex.Pattern;

import kg.yarrrkady.banktransfersdemo.domain.exception.card.InvalidCardNumberException;

public record CardNumber(String value) {

    private static final Pattern DIGITS = Pattern.compile("\\d{13,19}");
    
    public CardNumber {
        if (value == null || !DIGITS.matcher(value).matches() || !passesLuhn(value)) {
            throw new InvalidCardNumberException("Card number is invalid");
        }
    }

    public String masked() {
        return value.substring(0,6)
            + "*".repeat(value.length() - 10)
            + value.substring(value.length() - 4);
    }

    @Override
    public String toString() {
        return masked();
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

}
