package com.phh.fx_service.rate;

import java.math.BigDecimal;

public interface ExchangeRateProvider {

	BigDecimal getRate(
			String from,
			String to
	);
}