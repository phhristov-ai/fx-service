package com.phh.fx_service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.phh.fx_service.balance.Balance;
import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import com.phh.fx_service.conversion.ConversionRepository;
import com.phh.fx_service.idempotency.IdempotencyKeyRepository;
import com.phh.fx_service.rate.ExchangeRateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.cache.type=none"
})
@AutoConfigureMockMvc
@Testcontainers
class ConversionIntegrationTest {

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

	@MockitoBean
	private ExchangeRateProvider exchangeRateProvider;

	@BeforeEach
	void setUp() {
		idempotencyKeyRepository.deleteAll();
		conversionRepository.deleteAll();
		balanceRepository.deleteAll();
		clientRepository.deleteAll();
	}

	@Test
	void shouldConvertSuccessfully() throws Exception {

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

		balanceRepository.saveAll(List.of(usd, eur));

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		mockMvc.perform(
						post("/conversions")
								.header("X-Client-Id", "CLIENT-001")
								.header("Idempotency-Key", "test-key-001")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                            {
                              "sourceCurrency": "USD",
                              "targetCurrency": "EUR",
                              "amount": 100.00
                            }
                            """)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sourceCurrency")
						.value("USD"))
				.andExpect(jsonPath("$.targetCurrency")
						.value("EUR"))
				.andExpect(jsonPath("$.sourceAmount")
						.value(100.00))
				.andExpect(jsonPath("$.targetAmount")
						.value(85.00));

		Balance updatedUsd =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"USD"
						)
						.orElseThrow();

		Balance updatedEur =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"EUR"
						)
						.orElseThrow();

		assertThat(updatedUsd.getAmount())
				.isEqualByComparingTo("9900.0000");

		assertThat(updatedEur.getAmount())
				.isEqualByComparingTo("8085.0000");

		assertThat(conversionRepository.count())
				.isEqualTo(1);

		assertThat(
				idempotencyKeyRepository.findByClientIdAndKey(
						"CLIENT-001",
						"test-key-001"
				)
		).isPresent();
	}

	@Test
	void shouldReturnSameResultWhenIdempotencyKeyIsReplayed()
			throws Exception {

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

		balanceRepository.saveAll(List.of(usd, eur));

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		String request = """
        {
          "sourceCurrency": "USD",
          "targetCurrency": "EUR",
          "amount": 100.00
        }
        """;

		MvcResult firstResult =
				mockMvc.perform(
								post("/conversions")
										.header("X-Client-Id", "CLIENT-001")
										.header("Idempotency-Key", "test-key-001")
										.contentType(MediaType.APPLICATION_JSON)
										.content(request)
						)
						.andExpect(status().isOk())
						.andReturn();

		MvcResult secondResult =
				mockMvc.perform(
								post("/conversions")
										.header("X-Client-Id", "CLIENT-001")
										.header("Idempotency-Key", "test-key-001")
										.contentType(MediaType.APPLICATION_JSON)
										.content(request)
						)
						.andExpect(status().isOk())
						.andReturn();

		JsonNode firstJson =
				new ObjectMapper().readTree(
						firstResult.getResponse().getContentAsString()
				);

		JsonNode secondJson =
				new ObjectMapper().readTree(
						secondResult.getResponse().getContentAsString()
				);

		assertThat(
				firstJson.get("transactionId").asText()
		).isEqualTo(
				secondJson.get("transactionId").asText()
		);

		assertThat(conversionRepository.count())
				.isEqualTo(1);

		Balance updatedUsd =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"USD"
						)
						.orElseThrow();

		Balance updatedEur =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"EUR"
						)
						.orElseThrow();

		assertThat(updatedUsd.getAmount())
				.isEqualByComparingTo("9900.0000");

		assertThat(updatedEur.getAmount())
				.isEqualByComparingTo("8085.0000");

		assertThat(idempotencyKeyRepository
				.findByClientIdAndKey(
						"CLIENT-001",
						"test-key-001"
				))
				.isPresent();

		verify(exchangeRateProvider, times(1))
				.getRate("USD", "EUR");
	}

	@Test
	void shouldRejectConversionWhenFundsAreInsufficient()
			throws Exception {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Balance usd = new Balance();
		usd.setClient(client);
		usd.setCurrency("USD");
		usd.setAmount(new BigDecimal("50.0000"));

		Balance eur = new Balance();
		eur.setClient(client);
		eur.setCurrency("EUR");
		eur.setAmount(new BigDecimal("8000.0000"));

		balanceRepository.saveAll(List.of(usd, eur));

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		mockMvc.perform(
						post("/conversions")
								.header("X-Client-Id", "CLIENT-001")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                    {
                      "sourceCurrency": "USD",
                      "targetCurrency": "EUR",
                      "amount": 100.00
                    }
                    """)
				)
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code")
						.value("INSUFFICIENT_FUNDS"));

		Balance updatedUsd =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"USD"
						)
						.orElseThrow();

		Balance updatedEur =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"EUR"
						)
						.orElseThrow();

		assertThat(updatedUsd.getAmount())
				.isEqualByComparingTo("50.0000");

		assertThat(updatedEur.getAmount())
				.isEqualByComparingTo("8000.0000");

		assertThat(conversionRepository.count())
				.isZero();
	}

	@Test
	void shouldHandleConcurrentConversionsSafely() {

		Client client = new Client();
		client.setClientId("CLIENT-001");
		client = clientRepository.save(client);

		Balance usd = new Balance();
		usd.setClient(client);
		usd.setCurrency("USD");
		usd.setAmount(new BigDecimal("100.0000"));

		Balance eur = new Balance();
		eur.setClient(client);
		eur.setCurrency("EUR");
		eur.setAmount(new BigDecimal("0.0000"));

		balanceRepository.saveAll(List.of(usd, eur));

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		int numberOfRequests = 2;

		ExecutorService executor =
				Executors.newFixedThreadPool(numberOfRequests);

		CountDownLatch startLatch =
				new CountDownLatch(1);

		try {
			List<Future<Integer>> results =
					new ArrayList<>();

			for (int i = 0; i < numberOfRequests; i++) {

				String idempotencyKey =
						"concurrent-key-" + i;

				results.add(
						executor.submit(() -> {

							startLatch.await();

							return mockMvc.perform(
											post("/conversions")
													.header(
															"X-Client-Id",
															"CLIENT-001"
													)
													.header(
															"Idempotency-Key",
															idempotencyKey
													)
													.contentType(
															MediaType.APPLICATION_JSON
													)
													.content("""
                                        {
                                          "sourceCurrency": "USD",
                                          "targetCurrency": "EUR",
                                          "amount": 100.00
                                        }
                                        """)
									)
									.andReturn()
									.getResponse()
									.getStatus();
						})
				);
			}

			// Release both requests at approximately the same time.
			startLatch.countDown();

			List<Integer> statuses = results.stream()
					.map(future -> {
						try {
							return future.get();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					})
					.toList();

			assertThat(statuses)
					.containsExactlyInAnyOrder(
							200,
							422
					);

		} finally {
			executor.shutdown();
		}

		Balance updatedUsd =
				balanceRepository.findByClientClientIdAndCurrency(
						"CLIENT-001",
						"USD"
				).orElseThrow();

		Balance updatedEur =
				balanceRepository.findByClientClientIdAndCurrency(
						"CLIENT-001",
						"EUR"
				).orElseThrow();

		assertThat(updatedUsd.getAmount())
				.isEqualByComparingTo("0.0000");

		assertThat(updatedEur.getAmount())
				.isEqualByComparingTo("85.0000");

		assertThat(conversionRepository.count())
				.isEqualTo(1);
	}

	@Test
	void shouldHandleConcurrentRequestsWithSameIdempotencyKey() throws Exception {

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

		balanceRepository.saveAll(List.of(usd, eur));

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		int numberOfRequests = 2;

		ExecutorService executor =
				Executors.newFixedThreadPool(numberOfRequests);

		CountDownLatch startLatch =
				new CountDownLatch(1);

		try {
			List<Future<MvcResult>> results =
					new ArrayList<>();

			for (int i = 0; i < numberOfRequests; i++) {

				results.add(
						executor.submit(() -> {

							startLatch.await();

							return mockMvc.perform(
											post("/conversions")
													.header(
															"X-Client-Id",
															"CLIENT-001"
													)
													.header(
															"Idempotency-Key",
															"same-key-001"
													)
													.contentType(
															MediaType.APPLICATION_JSON
													)
													.content("""
                                        {
                                          "sourceCurrency": "USD",
                                          "targetCurrency": "EUR",
                                          "amount": 100.00
                                        }
                                        """)
									)
									.andReturn();
						})
				);
			}

			startLatch.countDown();

			List<MvcResult> responses =
					results.stream()
							.map(future -> {
								try {
									return future.get();
								} catch (Exception e) {
									throw new RuntimeException(e);
								}
							})
							.toList();

			assertThat(responses)
					.hasSize(2);

			assertThat(
					responses.get(0)
							.getResponse()
							.getStatus()
			).isEqualTo(200);

			assertThat(
					responses.get(1)
							.getResponse()
							.getStatus()
			).isEqualTo(200);

			String transactionId1 =
					JsonPath.read(
							responses.get(0)
									.getResponse()
									.getContentAsString(),
							"$.transactionId"
					);

			String transactionId2 =
					JsonPath.read(
							responses.get(1)
									.getResponse()
									.getContentAsString(),
							"$.transactionId"
					);

			assertThat(transactionId1)
					.isEqualTo(transactionId2);

		} finally {
			executor.shutdown();
		}

		Balance updatedUsd =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"USD"
						)
						.orElseThrow();

		Balance updatedEur =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"EUR"
						)
						.orElseThrow();

		assertThat(updatedUsd.getAmount())
				.isEqualByComparingTo("9900.0000");

		assertThat(updatedEur.getAmount())
				.isEqualByComparingTo("8085.0000");

		assertThat(conversionRepository.count())
				.isEqualTo(1);

		assertThat(
				idempotencyKeyRepository.findByClientIdAndKey(
						"CLIENT-001",
						"same-key-001"
				)
		).isPresent();
	}

	@Test
	void shouldReplayConversionForSameIdempotencyKey() throws Exception {

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

		balanceRepository.saveAll(List.of(usd, eur));

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		String request = """
        {
          "sourceCurrency": "USD",
          "targetCurrency": "EUR",
          "amount": 100.00
        }
        """;

		MvcResult firstResult =
				mockMvc.perform(
								post("/conversions")
										.header("X-Client-Id", "CLIENT-001")
										.header("Idempotency-Key", "replay-key-001")
										.contentType(MediaType.APPLICATION_JSON)
										.content(request)
						)
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.sourceCurrency")
								.value("USD"))
						.andExpect(jsonPath("$.targetCurrency")
								.value("EUR"))
						.andExpect(jsonPath("$.sourceAmount")
								.value(100.00))
						.andExpect(jsonPath("$.targetAmount")
								.value(85.00))
						.andReturn();

		MvcResult secondResult =
				mockMvc.perform(
								post("/conversions")
										.header("X-Client-Id", "CLIENT-001")
										.header("Idempotency-Key", "replay-key-001")
										.contentType(MediaType.APPLICATION_JSON)
										.content(request)
						)
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.sourceCurrency")
								.value("USD"))
						.andExpect(jsonPath("$.targetCurrency")
								.value("EUR"))
						.andExpect(jsonPath("$.sourceAmount")
								.value(100.00))
						.andExpect(jsonPath("$.targetAmount")
								.value(85.00))
						.andReturn();

		String firstTransactionId =
				JsonPath.read(
						firstResult.getResponse().getContentAsString(),
						"$.transactionId"
				);

		String secondTransactionId =
				JsonPath.read(
						secondResult.getResponse().getContentAsString(),
						"$.transactionId"
				);

		assertThat(secondTransactionId)
				.isEqualTo(firstTransactionId);

		assertThat(conversionRepository.count())
				.isEqualTo(1);

		assertThat(idempotencyKeyRepository
				.findByClientIdAndKey(
						"CLIENT-001",
						"replay-key-001"
				))
				.isPresent();

		Balance updatedUsd =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"USD"
						)
						.orElseThrow();

		Balance updatedEur =
				balanceRepository
						.findByClientClientIdAndCurrency(
								"CLIENT-001",
								"EUR"
						)
						.orElseThrow();

		assertThat(updatedUsd.getAmount())
				.isEqualByComparingTo("9900.0000");

		assertThat(updatedEur.getAmount())
				.isEqualByComparingTo("8085.0000");
	}
}


