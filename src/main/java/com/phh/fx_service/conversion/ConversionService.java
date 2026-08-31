package com.phh.fx_service.conversion;

import com.phh.fx_service.balance.Balance;
import com.phh.fx_service.balance.BalanceMapper;
import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.balance.BalanceResponse;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import com.phh.fx_service.conversion.dto.ConversionHistoryResponse;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import com.phh.fx_service.conversion.dto.CreateConversionRequest;
import com.phh.fx_service.exception.*;
import com.phh.fx_service.idempotency.IdempotencyKey;
import com.phh.fx_service.idempotency.IdempotencyKeyRepository;
import com.phh.fx_service.validation.CurrencyValidator;
import com.phh.fx_service.rate.RateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversionService {

	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final ConversionRepository conversionRepository;
	private final RateService rateService;
	private final ConversionMapper conversionMapper;
	private final CurrencyValidator currencyValidator;
	private final ConversionTransactionService transactionService;

	public ConversionResponse convert(
			String clientId,
			String idempotencyKey,
			CreateConversionRequest request
	) {

		validateRequest(request);

		Optional<ConversionResponse> existing =
				findExistingConversion(
						clientId,
						idempotencyKey
				);

		if (existing.isPresent()) {
			return existing.get();
		}

		String sourceCurrency =
				request.sourceCurrency()
						.toUpperCase(Locale.ROOT);

		String targetCurrency =
				request.targetCurrency()
						.toUpperCase(Locale.ROOT);

		BigDecimal sourceAmount =
				request.amount()
						.setScale(
								4,
								RoundingMode.UNNECESSARY
						);

		/*
		 * External network call happens outside
		 * the database transaction.
		 */
		BigDecimal rate =
				rateService.getRate(
						sourceCurrency,
						targetCurrency
				);

		return transactionService.execute(
				clientId,
				idempotencyKey,
				sourceCurrency,
				targetCurrency,
				sourceAmount,
				rate
		);
	}

	private void validateRequest(CreateConversionRequest request) {

		if (request == null) {
			throw new InvalidAmountException();
		}

		if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new InvalidAmountException();
		}

		if (request.amount().scale() > 4) {
			throw new InvalidAmountException(
					"Amount must have at most 4 decimal places"
			);
		}

		if (request.sourceCurrency() == null ||
				request.sourceCurrency().isBlank()) {
			throw new InvalidCurrencyException();
		}

		if (request.targetCurrency() == null ||
				request.targetCurrency().isBlank()) {
			throw new InvalidCurrencyException();
		}

		String source =
				request.sourceCurrency().toUpperCase(Locale.ROOT);

		String target =
				request.targetCurrency().toUpperCase(Locale.ROOT);

		currencyValidator.validate(source);
		currencyValidator.validate(target);

		if (source.equals(target)) {
			throw new InvalidCurrencyException(
					"Source and target currencies must be different"
			);
		}
	}

	private Optional<ConversionResponse> findExistingConversion(
			String clientId,
			String idempotencyKey
	) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			return Optional.empty();
		}

		Optional<IdempotencyKey> existing =
				idempotencyKeyRepository.findByClientIdAndKey(
						clientId,
						idempotencyKey
				);

		if (existing.isEmpty()) {
			return Optional.empty();
		}

		Conversion conversion =
				conversionRepository.findByTransactionId(
						existing.get().getTransactionId()
				).orElseThrow(() ->
						new IllegalStateException(
								"Idempotency key references missing conversion"
						));

		List<BalanceResponse> balances = List.of(
				new BalanceResponse(
						conversion.getSourceCurrency(),
						conversion.getSourceBalanceAfter()
				),
				new BalanceResponse(
						conversion.getTargetCurrency(),
						conversion.getTargetBalanceAfter()
				)
		);

		return Optional.of(
				conversionMapper.toResponse(
						conversion,
						balances
				)
		);
	}

	private Balance getBalance(
			Map<String, Balance> balancesByCurrency,
			String clientId,
			String currency
	) {
		Balance balance = balancesByCurrency.get(currency);

		if (balance == null) {
			throw new BalanceNotFoundException(
					clientId,
					currency
			);
		}

		return balance;
	}

	private void validateFunds(
			Balance sourceBalance,
			BigDecimal sourceAmount,
			String clientId,
			String sourceCurrency
	) {
		if (sourceBalance.getAmount()
				.compareTo(sourceAmount) < 0) {

			throw new InsufficientFundsException(
					clientId,
					sourceCurrency
			);
		}
	}

	public Page<ConversionHistoryResponse> findConversions(
			UUID transactionId,
			LocalDate date,
			String clientId,
			Pageable pageable
	) {

		List<Specification<Conversion>> specifications =
				new ArrayList<>();

		if (transactionId != null) {
			specifications.add(
					ConversionSpecifications.hasTransactionId(
							transactionId
					)
			);
		}

		if (date != null) {
			specifications.add(
					ConversionSpecifications.createdOn(date)
			);
		}

		if (clientId != null && !clientId.isBlank()) {
			specifications.add(
					ConversionSpecifications.hasClientId(
							clientId
					)
			);
		}

		Specification<Conversion> specification =
				specifications.stream()
						.reduce(Specification::and)
						.orElseThrow(
								InvalidConversionHistoryFilterException::new
						);

		return conversionRepository
				.findAll(specification, pageable)
				.map(conversionMapper::toHistoryResponse);
	}
}