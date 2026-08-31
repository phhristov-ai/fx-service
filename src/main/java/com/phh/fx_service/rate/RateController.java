package com.phh.fx_service.rate;

import com.phh.fx_service.rate.dto.RateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rates")
public class RateController {

	private final RateService rateService;

	@GetMapping
	public RateResponse getRate(
			@RequestParam String from,
			@RequestParam String to
	) {
		BigDecimal rate =
				rateService.getRate(from, to);

		return new RateResponse(
				from.toUpperCase(Locale.ROOT),
				to.toUpperCase(Locale.ROOT),
				rate
		);
	}
}
