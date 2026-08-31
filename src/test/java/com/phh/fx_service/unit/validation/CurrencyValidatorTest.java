package com.phh.fx_service.unit.validation;

import com.phh.fx_service.exception.InvalidCurrencyException;
import com.phh.fx_service.validation.CurrencyValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrencyValidatorTest {

	private final CurrencyValidator validator =
			new CurrencyValidator();

	@Test
	void shouldAcceptValidCurrency() {

		assertDoesNotThrow(
				() -> validator.validate("USD")
		);

		assertDoesNotThrow(
				() -> validator.validate("EUR")
		);

		assertDoesNotThrow(
				() -> validator.validate("GBP")
		);
	}
	@Test
	void shouldAcceptLowercaseCurrency() {

		assertDoesNotThrow(
				() -> validator.validate("usd")
		);
	}

	@Test
	void shouldRejectBlankCurrency() {

		assertThrows(
				InvalidCurrencyException.class,
				() -> validator.validate(" ")
		);
	}

	@Test
	void shouldRejectInvalidCurrency() {

		assertThrows(
				InvalidCurrencyException.class,
				() -> validator.validate("XYZ")
		);
	}

	@Test
	void shouldRejectNoCurrencyCode() {

		assertThrows(
				InvalidCurrencyException.class,
				() -> validator.validate("XXX")
		);
	}

	@Test
	void shouldRejectCurrencyWithWrongLength() {

		assertThrows(
				InvalidCurrencyException.class,
				() -> validator.validate("US")
		);

		assertThrows(
				InvalidCurrencyException.class,
				() -> validator.validate("USDD")
		);
	}
}