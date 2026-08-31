package com.phh.fx_service.conversion;

import com.phh.fx_service.conversion.dto.ConversionHistoryResponse;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import com.phh.fx_service.conversion.dto.CreateConversionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/conversions")
public class ConversionController {

	private final ConversionService conversionService;

	@PostMapping
	public ConversionResponse convert(
			@RequestHeader("X-Client-Id") String clientId,
			@RequestHeader(value = "Idempotency-Key", required = false)
			String idempotencyKey,
			@Valid @RequestBody CreateConversionRequest request
	) {
		return conversionService.convert(
				clientId,
				idempotencyKey,
				request
		);
	}

	@GetMapping
	public Page<ConversionHistoryResponse> findConversions(
			@RequestParam(required = false) UUID transactionId,
			@RequestParam(required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
			LocalDate date,
			@RequestParam(required = false) String clientId,
			@PageableDefault(size = 20)
			Pageable pageable
	) {
		return conversionService.findConversions(
				transactionId,
				date,
				clientId,
				pageable
		);
	}
}
