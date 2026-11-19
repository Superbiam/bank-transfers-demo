package kg.yarrrkady.banktransfersdemo;

import org.springframework.boot.SpringApplication;

public class TestBankTransfersDemoApplication {

	public static void main(String[] args) {
		SpringApplication.from(BankTransfersDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
