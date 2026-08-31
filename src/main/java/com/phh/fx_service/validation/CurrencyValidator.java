package com.phh.fx_service.validation;

import com.phh.fx_service.exception.InvalidCurrencyException;
import org.springframework.stereotype.Component;

import java.util.Currency;
import java.util.Locale;

@Component
public class CurrencyValidator {

	public void validate(String currency) {

		if (currency == null || currency.isBlank()) {
			throw new InvalidCurrencyException();
		}

		String normalized =
				currency.toUpperCase(Locale.ROOT);

		if ("XXX".equals(normalized)) {
			throw new InvalidCurrencyException(normalized);
		}

		try {
			Currency.getInstance(normalized);
		} catch (IllegalArgumentException e) {
			throw new InvalidCurrencyException(normalized);
		}
	}
}