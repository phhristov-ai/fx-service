package com.phh.fx_service.exception;

import java.time.Instant;

public record ErrorResponse(
		String code,
		String message,
		Instant timestamp
) {
}