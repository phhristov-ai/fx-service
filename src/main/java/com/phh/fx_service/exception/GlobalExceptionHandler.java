package com.phh.fx_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ClientNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleClientNotFound(
			ClientNotFoundException exception
	) {
		return build(
				HttpStatus.NOT_FOUND,
				"CLIENT_NOT_FOUND",
				exception.getMessage()
		);
	}

	@ExceptionHandler(BalanceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleBalanceNotFound(
			BalanceNotFoundException exception
	) {
		return build(
				HttpStatus.NOT_FOUND,
				"BALANCE_NOT_FOUND",
				exception.getMessage()
		);
	}

	@ExceptionHandler(InsufficientFundsException.class)
	public ResponseEntity<ErrorResponse> handleInsufficientFunds(
			InsufficientFundsException exception
	) {
		return build(
				HttpStatus.UNPROCESSABLE_ENTITY,
				"INSUFFICIENT_FUNDS",
				exception.getMessage()
		);
	}

	@ExceptionHandler(InvalidAmountException.class)
	public ResponseEntity<ErrorResponse> handleInvalidAmount(
			InvalidAmountException exception
	) {
		return build(
				HttpStatus.BAD_REQUEST,
				"INVALID_AMOUNT",
				exception.getMessage()
		);
	}

	@ExceptionHandler(InvalidCurrencyException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCurrency(
			InvalidCurrencyException exception
	) {
		return build(
				HttpStatus.BAD_REQUEST,
				"INVALID_CURRENCY",
				exception.getMessage()
		);
	}

	@ExceptionHandler(RateProviderException.class)
	public ResponseEntity<ErrorResponse> handleRateProvider(
			RateProviderException exception
	) {
		return build(
				HttpStatus.SERVICE_UNAVAILABLE,
				"RATE_PROVIDER_UNAVAILABLE",
				exception.getMessage()
		);
	}

	private ResponseEntity<ErrorResponse> build(
			HttpStatus status,
			String code,
			String message
	) {
		ErrorResponse response = new ErrorResponse(
				code,
				message,
				Instant.now()
		);

		return ResponseEntity
				.status(status)
				.body(response);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(
			MethodArgumentNotValidException exception
	) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(error ->
						error.getField() + ": " + error.getDefaultMessage()
				)
				.findFirst()
				.orElse("Validation failed");

		return build(
				HttpStatus.BAD_REQUEST,
				"VALIDATION_ERROR",
				message
		);
	}

	@ExceptionHandler(InvalidConversionHistoryFilterException.class)
	public ResponseEntity<ErrorResponse> handleInvalidConversionHistoryFilter(
			InvalidConversionHistoryFilterException exception
	) {
		return build(
				HttpStatus.BAD_REQUEST,
				"INVALID_CONVERSION_HISTORY_FILTER",
				exception.getMessage()
		);
	}
}