package com.phh.fx_service.exception;

public class BalanceNotFoundException extends RuntimeException {

	public BalanceNotFoundException(
			String clientId,
			String currency
	) {
		super(
				"Balance not found for client "
						+ clientId
						+ " and currency "
						+ currency
		);
	}
}
