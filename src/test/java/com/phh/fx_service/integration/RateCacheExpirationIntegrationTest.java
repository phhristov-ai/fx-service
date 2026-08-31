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

@SpringBootTest(properties = {
		"spring.cache.redis.time-to-live=1s"
})
@AutoConfigureMockMvc
@Testcontainers
class RateCacheExpirationIntegrationTest {

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
	void shouldCallProviderAgainAfterCacheExpires()
			throws Exception {

		when(exchangeRateProvider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"))
				.thenReturn(new BigDecimal("0.8600"));

		// First request
		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(0.8500));

		// Wait for Redis TTL to expire
		Thread.sleep(1500);

		// Second request
		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rate").value(0.8600));

		verify(exchangeRateProvider, times(2))
				.getRate("USD", "EUR");
	}
}
