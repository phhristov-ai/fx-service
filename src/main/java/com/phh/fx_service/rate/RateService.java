package com.phh.fx_service.rate;

import com.phh.fx_service.exception.InvalidCurrencyException;
import com.phh.fx_service.validation.CurrencyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RateService {

	private final ExchangeRateProvider provider;
	private final CurrencyValidator currencyValidator;

	@Cacheable(
			value = "exchangeRates",
			key = "#from + '-' + #to"
	)
	public BigDecimal getRate(
			String from,
			String to
	) {
		currencyValidator.validate(from);
		currencyValidator.validate(to);

		String source =
				from.toUpperCase(Locale.ROOT);

		String target =
				to.toUpperCase(Locale.ROOT);

		if (source.equals(target)) {
			throw new InvalidCurrencyException(
					"Source and target currencies must be different"
			);
		}

		return provider.getRate(source, target);
	}
}