package com.phh.fx_service.exception;

public class InvalidConversionHistoryFilterException
		extends RuntimeException {

	public InvalidConversionHistoryFilterException() {
		super(
				"At least one filter must be provided: " +
						"transactionId, date, or clientId"
		);
	}
}