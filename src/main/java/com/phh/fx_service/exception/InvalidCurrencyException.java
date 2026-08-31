package com.phh.fx_service.exception;

public class InvalidCurrencyException extends RuntimeException {

	public InvalidCurrencyException() {
		super("Invalid currency");
	}

	public InvalidCurrencyException(String currency) {
		super("Invalid currency: " + currency);
	}
}