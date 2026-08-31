package com.phh.fx_service.rate;

import com.phh.fx_service.exception.RateProviderException;
import com.phh.fx_service.rate.dto.FrankfurterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class FrankfurterExchangeRateProvider
		implements ExchangeRateProvider {

	private final RestClient restClient;

	@Override
	@Retryable(
			retryFor = RateProviderException.class,
			maxAttempts = 3,
			backoff = @Backoff(delay = 200)
	)
	public BigDecimal getRate(
			String from,
			String to
	) {
		try {
			FrankfurterResponse response =
					restClient.get()
							.uri(uriBuilder -> uriBuilder
									.path("/v1/latest")
									.queryParam("from", from)
									.build())
							.retrieve()
							.body(FrankfurterResponse.class);

			if (response == null ||
					response.rates() == null) {
				throw new RateProviderException(
						"Invalid response from FX provider"
				);
			}

			BigDecimal rate =
					response.rates().get(to);

			if (rate == null) {
				throw new RateProviderException(
						"Exchange rate not available for "
								+ from + " -> " + to
				);
			}

			return rate;

		} catch (RateProviderException e) {
			throw e;
		} catch (Exception e) {
			throw new RateProviderException(
					"Failed to retrieve exchange rate",
					e
			);
		}
	}
}