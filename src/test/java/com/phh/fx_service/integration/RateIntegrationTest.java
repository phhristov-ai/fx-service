package com.phh.fx_service.integration;

import com.phh.fx_service.rate.ExchangeRateProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.cache.type=none"
})
@AutoConfigureMockMvc
@Testcontainers
class RateIntegrationTest {

	@MockitoBean
	private ExchangeRateProvider exchangeRateProvider;

	@Autowired
	private MockMvc mockMvc;

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:18");

	@Test
	void shouldGetExchangeRate() throws Exception {

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.from").value("USD"))
				.andExpect(jsonPath("$.to").value("EUR"))
				.andExpect(jsonPath("$.rate").value(0.8500));

		verify(exchangeRateProvider)
				.getRate("USD", "EUR");
	}

	@Test
	void shouldRejectInvalidCurrencyForRate() throws Exception {

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "XXX")
				)
				.andExpect(status().isBadRequest());

		verifyNoInteractions(exchangeRateProvider);
	}

	@Test
	void shouldRejectSameCurrencyForRate() throws Exception {

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "USD")
				)
				.andExpect(status().isBadRequest());

		verifyNoInteractions(exchangeRateProvider);
	}

}
