package com.phh.fx_service.conversion.dto;

import com.phh.fx_service.balance.BalanceResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversionResponse(
		UUID transactionId,
		BigDecimal sourceAmount,
		String sourceCurrency,
		BigDecimal targetAmount,
		String targetCurrency,
		BigDecimal rate,
		Instant timestamp,
		List<BalanceResponse> balances
) {
}