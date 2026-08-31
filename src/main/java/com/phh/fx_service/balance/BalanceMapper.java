package com.phh.fx_service.balance;

import org.springframework.stereotype.Component;

@Component
public class BalanceMapper {

	public BalanceResponse toResponse(Balance balance) {
		return new BalanceResponse(
				balance.getCurrency(),
				balance.getAmount()
		);
	}
}