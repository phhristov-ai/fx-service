package com.phh.fx_service.conversion.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateConversionRequest(

		@NotBlank
		String sourceCurrency,

		@NotBlank
		String targetCurrency,

		@NotNull
		@Positive
		@Digits(integer = 15, fraction = 4)
		BigDecimal amount
) {
}