package com.phh.fx_service.integration;

import com.phh.fx_service.balance.Balance;
import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ClientIntegrationTest {

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private BalanceRepository balanceRepository;

	@Autowired
	private MockMvc mockMvc;

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:18");

	@BeforeEach
	void setUp() {
		balanceRepository.deleteAll();
		clientRepository.deleteAll();
	}

	@Test
	void shouldGetClientBalances() throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");

		client = clientRepository.save(client);

		Balance usd = new Balance();
		usd.setClient(client);
		usd.setCurrency("USD");
		usd.setAmount(new BigDecimal("10000.0000"));

		Balance eur = new Balance();
		eur.setClient(client);
		eur.setCurrency("EUR");
		eur.setAmount(new BigDecimal("8000.0000"));

		balanceRepository.saveAll(
				List.of(usd, eur)
		);

		mockMvc.perform(
						get("/clients/CLIENT-001/balances")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].currency").value("USD"))
				.andExpect(jsonPath("$[0].amount").value(10000.0000))
				.andExpect(jsonPath("$[1].currency").value("EUR"))
				.andExpect(jsonPath("$[1].amount").value(8000.0000));
	}

	@Test
	void shouldRejectUnknownClientForBalances() throws Exception {

		mockMvc.perform(
						get("/clients/UNKNOWN-001/balances")
				)
				.andExpect(status().isNotFound());
	}
}
