package com.phh.fx_service.integration;

import com.phh.fx_service.exception.RateProviderException;
import com.phh.fx_service.rate.ExchangeRateProvider;
import com.phh.fx_service.rate.dto.FrankfurterResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest
class FrankfurterExchangeRateProviderRetryTest {

	@Autowired
	private ExchangeRateProvider provider;

	@MockitoBean
	private RestClient restClient;

	@SuppressWarnings("rawtypes")
	@MockitoBean
	private RestClient.RequestHeadersUriSpec requestSpec;

	@MockitoBean
	private RestClient.ResponseSpec responseSpec;

	@Test
	void shouldRetryWhenProviderFails() {

		FrankfurterResponse response =
				new FrankfurterResponse(
						BigDecimal.ONE,
						"USD",
						LocalDate.now(),
						Map.of(
								"EUR",
								new BigDecimal("0.8500")
						)
				);

		when(restClient.get())
				.thenReturn(requestSpec);

		when(requestSpec.uri(any(Function.class)))
				.thenReturn(requestSpec);

		when(requestSpec.retrieve())
				.thenReturn(responseSpec);

		when(responseSpec.body(FrankfurterResponse.class))
				.thenThrow(
						new RateProviderException("Provider unavailable")
				)
				.thenThrow(
						new RateProviderException("Provider unavailable")
				)
				.thenReturn(response);

		BigDecimal result =
				provider.getRate("USD", "EUR");

		assertThat(result)
				.isEqualByComparingTo("0.8500");

		verify(responseSpec, times(3))
				.body(FrankfurterResponse.class);
	}

	@Test
	void shouldFailAfterMaximumRetryAttempts() {

		when(restClient.get())
				.thenReturn(requestSpec);

		when(requestSpec.uri(any(Function.class)))
				.thenReturn(requestSpec);

		when(requestSpec.retrieve())
				.thenReturn(responseSpec);

		when(responseSpec.body(FrankfurterResponse.class))
				.thenThrow(
						new RateProviderException("Provider unavailable")
				);

		assertThatThrownBy(
				() -> provider.getRate("USD", "EUR")
		)
				.isInstanceOf(RateProviderException.class);

		verify(responseSpec, times(3))
				.body(FrankfurterResponse.class);
	}
}