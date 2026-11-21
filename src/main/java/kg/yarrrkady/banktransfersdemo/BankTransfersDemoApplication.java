package kg.yarrrkady.banktransfersdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BankTransfersDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankTransfersDemoApplication.class, args);
	}

}
