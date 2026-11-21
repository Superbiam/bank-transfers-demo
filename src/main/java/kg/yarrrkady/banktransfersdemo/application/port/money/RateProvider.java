package kg.yarrrkady.banktransfersdemo.application.port.money;

import java.util.Currency;

import kg.yarrrkady.banktransfersdemo.domain.entity.money.ExchangeRate;

public interface RateProvider {
    ExchangeRate rate(Currency from, Currency to); 
}
