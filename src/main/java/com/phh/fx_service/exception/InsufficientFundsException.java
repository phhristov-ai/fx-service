package com.phh.fx_service.exception;

public class InsufficientFundsException extends RuntimeException {

	public InsufficientFundsException(
			String clientId,
			String currency
	) {
		super(
				"Insufficient funds for client "
						+ clientId
						+ " in currency "
						+ currency
		);
	}
}