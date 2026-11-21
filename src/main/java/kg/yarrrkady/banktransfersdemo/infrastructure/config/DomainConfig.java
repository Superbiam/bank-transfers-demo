package kg.yarrrkady.banktransfersdemo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import kg.yarrrkady.banktransfersdemo.domain.entity.transfer.TransferDomainService;

@Configuration
class DomainConfig {

    @Bean
    TransferDomainService transferDomainService() {
        return new TransferDomainService();
    }
}
