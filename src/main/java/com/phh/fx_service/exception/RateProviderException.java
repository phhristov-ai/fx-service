package com.phh.fx_service.exception;

public class RateProviderException extends RuntimeException {

	public RateProviderException(String message) {
		super(message);
	}

	public RateProviderException(
			String message,
			Throwable cause
	) {
		super(message, cause);
	}
}