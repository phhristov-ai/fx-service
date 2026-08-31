package com.phh.fx_service.rate.dto;

import java.math.BigDecimal;

public record RateResponse(
		String from,
		String to,
		BigDecimal rate
) {
}