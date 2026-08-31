package com.phh.fx_service.unit.rate;

import com.phh.fx_service.exception.RateProviderException;
import com.phh.fx_service.rate.FrankfurterExchangeRateProvider;
import com.phh.fx_service.rate.dto.FrankfurterResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrankfurterExchangeRateProviderTest {

	@Mock
	private RestClient restClient;

	@SuppressWarnings("rawtypes")
	@Mock
	private RestClient.RequestHeadersUriSpec requestSpec;

	@Mock
	private RestClient.RequestHeadersSpec<?> headersSpec;

	@Mock
	private RestClient.ResponseSpec responseSpec;

	private FrankfurterExchangeRateProvider provider;

	@BeforeEach
	void setUp() {
		provider = new FrankfurterExchangeRateProvider(restClient);
	}

	@Test
	void shouldReturnExchangeRate() {

		FrankfurterResponse response =
				new FrankfurterResponse(
						new BigDecimal("1.00"),
						"USD",
						LocalDate.of(2026, 8, 30),
						Map.of(
								"EUR",
								new BigDecimal("0.8500")
						)
				);

		when(restClient.get())
				.thenReturn(requestSpec);

		when(requestSpec.uri(any(Function.class)))
				.thenReturn(headersSpec);

		when(headersSpec.retrieve())
				.thenReturn(responseSpec);

		when(responseSpec.body(FrankfurterResponse.class))
				.thenReturn(response);

		BigDecimal result =
				provider.getRate("USD", "EUR");

		assertThat(result)
				.isEqualByComparingTo("0.8500");
	}

	@Test
	void shouldRejectMissingExchangeRate() {

		FrankfurterResponse response =
				new FrankfurterResponse(
						new BigDecimal("1.00"),
						"USD",
						LocalDate.of(2026, 8, 30),
						Map.of(
								"GBP",
								new BigDecimal("0.78")
						)
				);

		when(restClient.get())
				.thenReturn(requestSpec);

		when(requestSpec.uri(any(Function.class)))
				.thenReturn(headersSpec);

		when(headersSpec.retrieve())
				.thenReturn(responseSpec);

		when(responseSpec.body(FrankfurterResponse.class))
				.thenReturn(response);

		RateProviderException exception =
				assertThrows(
						RateProviderException.class,
						() -> provider.getRate("USD", "EUR")
				);

		assertThat(exception)
				.hasMessageContaining("USD -> EUR");
	}

	@Test
	void shouldRejectResponseWithNullRates() {

		FrankfurterResponse response =
				new FrankfurterResponse(
						new BigDecimal("1.00"),
						"USD",
						LocalDate.of(2026, 8, 30),
						null
				);

		when(restClient.get())
				.thenReturn(requestSpec);

		when(requestSpec.uri(any(Function.class)))
				.thenReturn(headersSpec);

		when(headersSpec.retrieve())
				.thenReturn(responseSpec);

		when(responseSpec.body(FrankfurterResponse.class))
				.thenReturn(response);

		RateProviderException exception =
				assertThrows(
						RateProviderException.class,
						() -> provider.getRate("USD", "EUR")
				);

		assertThat(exception)
				.hasMessage("Invalid response from FX provider");
	}

	@Test
	void shouldWrapProviderFailure() {

		RuntimeException cause =
				new RuntimeException("Connection refused");

		when(restClient.get())
				.thenThrow(cause);

		RateProviderException exception =
				assertThrows(
						RateProviderException.class,
						() -> provider.getRate("USD", "EUR")
				);

		assertThat(exception)
				.hasMessage("Failed to retrieve exchange rate");

		assertThat(exception)
				.hasCause(cause);
	}

	@Test
	void shouldPropagateRateProviderException() {

		RateProviderException expected =
				new RateProviderException("Provider error");

		when(restClient.get())
				.thenThrow(expected);

		RateProviderException actual =
				assertThrows(
						RateProviderException.class,
						() -> provider.getRate("USD", "EUR")
				);

		assertSame(expected, actual);
	}
}