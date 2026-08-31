package com.phh.fx_service.balance;

import java.math.BigDecimal;

public record BalanceResponse(
		String currency,
		BigDecimal amount
) {
}