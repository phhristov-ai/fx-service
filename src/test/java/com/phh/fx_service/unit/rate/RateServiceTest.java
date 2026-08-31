package com.phh.fx_service.unit.rate;

import com.phh.fx_service.exception.InvalidCurrencyException;
import com.phh.fx_service.exception.RateProviderException;
import com.phh.fx_service.rate.ExchangeRateProvider;
import com.phh.fx_service.rate.RateService;
import com.phh.fx_service.validation.CurrencyValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateServiceTest {

	@Mock
	private ExchangeRateProvider provider;

	@Mock
	private CurrencyValidator currencyValidator;

	@InjectMocks
	private RateService rateService;

	@Test
	void shouldReturnExchangeRate() {

		when(provider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		BigDecimal result =
				rateService.getRate("USD", "EUR");

		assertThat(result)
				.isEqualByComparingTo("0.8500");

		verify(currencyValidator)
				.validate("USD");

		verify(currencyValidator)
				.validate("EUR");

		verify(provider)
				.getRate("USD", "EUR");
	}

	@Test
	void shouldNormalizeCurrenciesBeforeCallingProvider() {

		when(provider.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		BigDecimal result =
				rateService.getRate("usd", "eur");

		assertThat(result)
				.isEqualByComparingTo("0.8500");

		verify(provider)
				.getRate("USD", "EUR");
	}

	@Test
	void shouldRejectInvalidSourceCurrency() {

		doThrow(new InvalidCurrencyException("XXX"))
				.when(currencyValidator)
				.validate("XXX");

		assertThrows(
				InvalidCurrencyException.class,
				() -> rateService.getRate("XXX", "EUR")
		);

		verify(provider, never())
				.getRate(anyString(), anyString());
	}

	@Test
	void shouldRejectInvalidTargetCurrency() {

		doNothing()
				.when(currencyValidator)
				.validate("USD");

		doThrow(new InvalidCurrencyException("XXX"))
				.when(currencyValidator)
				.validate("XXX");

		assertThrows(
				InvalidCurrencyException.class,
				() -> rateService.getRate("USD", "XXX")
		);

		verify(currencyValidator).validate("USD");
		verify(currencyValidator).validate("XXX");

		verifyNoInteractions(provider);
	}

	@Test
	void shouldRejectSameCurrencies() {

		assertThrows(
				InvalidCurrencyException.class,
				() -> rateService.getRate("USD", "USD")
		);

		verify(provider, never())
				.getRate(anyString(), anyString());
	}

	@Test
	void shouldPropagateRateProviderException() {

		RateProviderException exception =
				new RateProviderException(
						"Provider unavailable"
				);

		when(provider.getRate("USD", "EUR"))
				.thenThrow(exception);

		assertThatThrownBy(() ->
				rateService.getRate("USD", "EUR")
		)
				.isSameAs(exception);
	}
}