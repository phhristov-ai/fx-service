package com.phh.fx_service.integration;

import com.phh.fx_service.rate.ExchangeRateProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Objects;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RateCacheIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CacheManager cacheManager;

	@MockitoBean
	private ExchangeRateProvider exchangeRateProvider;

	@BeforeEach
	void setUp() {
		Objects.requireNonNull(
				cacheManager.getCache("exchangeRates")
		).clear();
	}

	@Test
	void shouldCacheExchangeRate() throws Exception {

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		// First request -> provider should be called
		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(0.8500));

		// Second request -> should come from Redis
		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(0.8500));

		verify(exchangeRateProvider, times(1))
				.getRate("USD", "EUR");
	}

	@Test
	void shouldUseSeparateCacheEntriesForDifferentCurrencyPairs()
			throws Exception {

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		when(exchangeRateProvider.getRate("USD", "GBP"))
				.thenReturn(new BigDecimal("0.7500"));

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(0.8500));

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "GBP")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(0.7500));

		verify(exchangeRateProvider)
				.getRate("USD", "EUR");

		verify(exchangeRateProvider)
				.getRate("USD", "GBP");
	}

}
