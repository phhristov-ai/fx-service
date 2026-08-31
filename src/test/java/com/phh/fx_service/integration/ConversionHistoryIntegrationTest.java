package com.phh.fx_service.integration;

import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import com.phh.fx_service.conversion.Conversion;
import com.phh.fx_service.conversion.ConversionRepository;
import com.phh.fx_service.idempotency.IdempotencyKeyRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ConversionHistoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:18");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private BalanceRepository balanceRepository;

	@Autowired
	private ConversionRepository conversionRepository;

	@Autowired
	private IdempotencyKeyRepository idempotencyKeyRepository;

	@BeforeEach
	void setUp() {
		idempotencyKeyRepository.deleteAll();
		conversionRepository.deleteAll();
		balanceRepository.deleteAll();
		clientRepository.deleteAll();
	}

	@Test
	void shouldFindConversionsByClientId() throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Conversion conversion = createConversion(
				client,
				"USD",
				"EUR",
				"100.0000",
				"85.0000",
				"0.8500",
				Instant.now()
		);

		conversionRepository.save(conversion);

		mockMvc.perform(
						get("/conversions")
								.param("clientId", "CLIENT-001")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(
						jsonPath("$.content[0].transactionId")
								.value(conversion.getTransactionId().toString())
				)
				.andExpect(
						jsonPath("$.content[0].sourceCurrency")
								.value("USD")
				)
				.andExpect(
						jsonPath("$.content[0].targetCurrency")
								.value("EUR")
				)
				.andExpect(
						jsonPath("$.content[0].sourceAmount")
								.value(100.0000)
				)
				.andExpect(
						jsonPath("$.content[0].targetAmount")
								.value(85.0000)
				)
				.andExpect(
						jsonPath("$.content[0].rate")
								.value(0.8500)
				);
	}

	@Test
	void shouldFindConversionsByTransactionId() throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Conversion conversion = createConversion(
				client,
				"USD",
				"EUR",
				"100.0000",
				"85.0000",
				"0.8500",
				Instant.now()
		);

		conversionRepository.save(conversion);

		mockMvc.perform(
						get("/conversions")
								.param(
										"transactionId",
										conversion.getTransactionId().toString()
								)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(
						jsonPath("$.content[0].transactionId")
								.value(
										conversion
												.getTransactionId()
												.toString()
								)
				);
	}

	@Test
	void shouldFindConversionsByDate() throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Instant requestedDate =
				LocalDate.of(2026, 8, 31)
						.atStartOfDay(ZoneOffset.UTC)
						.plusHours(10)
						.toInstant();

		Instant otherDate =
				LocalDate.of(2026, 8, 30)
						.atStartOfDay(ZoneOffset.UTC)
						.plusHours(10)
						.toInstant();

		Conversion matching = createConversion(
				client,
				"USD",
				"EUR",
				"100.0000",
				"85.0000",
				"0.8500",
				requestedDate
		);

		Conversion notMatching = createConversion(
				client,
				"USD",
				"EUR",
				"200.0000",
				"170.0000",
				"0.8500",
				otherDate
		);

		conversionRepository.saveAll(
				List.of(matching, notMatching)
		);

		mockMvc.perform(
						get("/conversions")
								.param("date", "2026-08-31")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(
						jsonPath("$.content[0].transactionId")
								.value(
										matching
												.getTransactionId()
												.toString()
								)
				);
	}

	@Test
	void shouldFindConversionsWithMultipleFilters() throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Instant date =
				LocalDate.of(2026, 8, 31)
						.atStartOfDay(ZoneOffset.UTC)
						.plusHours(10)
						.toInstant();

		Conversion matching = createConversion(
				client,
				"USD",
				"EUR",
				"100.0000",
				"85.0000",
				"0.8500",
				date
		);

		Conversion other = createConversion(
				client,
				"USD",
				"EUR",
				"200.0000",
				"170.0000",
				"0.8500",
				date
		);

		conversionRepository.saveAll(
				List.of(matching, other)
		);

		mockMvc.perform(
						get("/conversions")
								.param(
										"transactionId",
										matching.getTransactionId().toString()
								)
								.param("clientId", "CLIENT-001")
								.param("date", "2026-08-31")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(
						jsonPath("$.content[0].transactionId")
								.value(
										matching
												.getTransactionId()
												.toString()
								)
				);
	}

	@Test
	void shouldRejectMissingHistoryFilters() throws Exception {

		mockMvc.perform(
						get("/conversions")
				)
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldPaginateConversionHistory() throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Instant now = Instant.now();

		Conversion first = createConversion(
				client,
				"USD",
				"EUR",
				"100.0000",
				"85.0000",
				"0.8500",
				now.minusSeconds(300)
		);

		Conversion second = createConversion(
				client,
				"USD",
				"EUR",
				"200.0000",
				"170.0000",
				"0.8500",
				now.minusSeconds(200)
		);

		Conversion third = createConversion(
				client,
				"USD",
				"EUR",
				"300.0000",
				"255.0000",
				"0.8500",
				now.minusSeconds(100)
		);

		conversionRepository.saveAll(
				List.of(first, second, third)
		);

		mockMvc.perform(
						get("/conversions")
								.param("clientId", "CLIENT-001")
								.param("page", "0")
								.param("size", "2")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.number").value(0));
	}

	private Conversion createConversion(
			Client client,
			String sourceCurrency,
			String targetCurrency,
			String sourceAmount,
			String targetAmount,
			String rate,
			Instant createdAt
	) {
		Conversion conversion = new Conversion();

		conversion.setTransactionId(UUID.randomUUID());
		conversion.setClient(client);
		conversion.setSourceCurrency(sourceCurrency);
		conversion.setSourceAmount(new BigDecimal(sourceAmount));
		conversion.setTargetCurrency(targetCurrency);
		conversion.setTargetAmount(new BigDecimal(targetAmount));
		conversion.setRate(new BigDecimal(rate));
		conversion.setCreatedAt(createdAt);

		conversion.setSourceBalanceAfter(
				new BigDecimal("9900.0000")
		);

		conversion.setTargetBalanceAfter(
				new BigDecimal("8085.0000")
		);

		return conversion;
	}

}
