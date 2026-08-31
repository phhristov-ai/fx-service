package com.phh.fx_service.conversion.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConversionHistoryResponse(
		UUID transactionId,
		BigDecimal sourceAmount,
		String sourceCurrency,
		BigDecimal targetAmount,
		String targetCurrency,
		BigDecimal rate,
		Instant timestamp
) {
}