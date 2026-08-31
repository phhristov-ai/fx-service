package com.phh.fx_service.exception;

public class InvalidAmountException extends RuntimeException {

	public InvalidAmountException() {
		super("Amount must be greater than zero");
	}

	public InvalidAmountException(String message) {
		super(message);
	}
}