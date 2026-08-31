package com.phh.fx_service.rate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record FrankfurterResponse(
		BigDecimal amount,
		String base,
		LocalDate date,
		Map<String, BigDecimal> rates
) {
}