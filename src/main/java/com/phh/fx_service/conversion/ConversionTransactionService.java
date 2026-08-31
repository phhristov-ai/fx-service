package com.phh.fx_service.conversion;

import com.phh.fx_service.balance.Balance;
import com.phh.fx_service.balance.BalanceMapper;
import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.balance.BalanceResponse;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import com.phh.fx_service.exception.BalanceNotFoundException;
import com.phh.fx_service.exception.ClientNotFoundException;
import com.phh.fx_service.exception.InsufficientFundsException;
import com.phh.fx_service.idempotency.IdempotencyKey;
import com.phh.fx_service.idempotency.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversionTransactionService {

	private final ClientRepository clientRepository;
	private final BalanceRepository balanceRepository;
	private final ConversionRepository conversionRepository;
	private final IdempotencyKeyRepository idempotencyKeyRepository;
	private final ConversionMapper conversionMapper;
	private final BalanceMapper balanceMapper;

	@Transactional
	public ConversionResponse execute(
			String clientId,
			String idempotencyKey,
			String sourceCurrency,
			String targetCurrency,
			BigDecimal sourceAmount,
			BigDecimal rate
	) {

		Client client =
				clientRepository.findByClientId(clientId)
						.orElseThrow(() ->
								new ClientNotFoundException(clientId));

		/*
		 * Acquire the pessimistic locks.
		 *
		 * From this point until commit, the relevant
		 * balance rows are protected from concurrent
		 * conversions.
		 */
		List<Balance> balances =
				balanceRepository.findForUpdate(
						clientId,
						List.of(
								sourceCurrency,
								targetCurrency
						)
				);

		/*
		 * Second idempotency check.
		 *
		 * Another request with the same key may have
		 * completed while this request was obtaining
		 * the exchange rate.
		 */
		Optional<ConversionResponse> existing =
				findExistingConversion(
						clientId,
						idempotencyKey
				);

		if (existing.isPresent()) {
			return existing.get();
		}

		Map<String, Balance> balancesByCurrency =
				balances.stream()
						.collect(Collectors.toMap(
								Balance::getCurrency,
								Function.identity()
						));

		Balance sourceBalance =
				getBalance(
						balancesByCurrency,
						clientId,
						sourceCurrency
				);

		Balance targetBalance =
				getBalance(
						balancesByCurrency,
						clientId,
						targetCurrency
				);

		validateFunds(
				sourceBalance,
				sourceAmount,
				clientId,
				sourceCurrency
		);

		BigDecimal targetAmount =
				sourceAmount
						.multiply(rate)
						.setScale(
								4,
								RoundingMode.HALF_UP
						);

		sourceBalance.setAmount(
				sourceBalance.getAmount()
						.subtract(sourceAmount)
		);

		targetBalance.setAmount(
				targetBalance.getAmount()
						.add(targetAmount)
		);

		Conversion conversion = new Conversion();

		conversion.setTransactionId(UUID.randomUUID());
		conversion.setClient(client);
		conversion.setSourceCurrency(sourceCurrency);
		conversion.setSourceAmount(sourceAmount);
		conversion.setTargetCurrency(targetCurrency);
		conversion.setTargetAmount(targetAmount);
		conversion.setRate(rate);
		conversion.setCreatedAt(Instant.now());

		conversion.setSourceBalanceAfter(
				sourceBalance.getAmount()
		);

		conversion.setTargetBalanceAfter(
				targetBalance.getAmount()
		);

		Conversion saved =
				conversionRepository.save(conversion);

		saveIdempotencyKey(
				clientId,
				idempotencyKey,
				saved
		);

		List<BalanceResponse> balanceResponses =
				balanceRepository
						.findAllByClientClientId(clientId)
						.stream()
						.map(balanceMapper::toResponse)
						.toList();

		return conversionMapper.toResponse(
				saved,
				balanceResponses
		);
	}

	private void saveIdempotencyKey(
			String clientId,
			String idempotencyKey,
			Conversion conversion
	) {
		if (idempotencyKey == null ||
				idempotencyKey.isBlank()) {
			return;
		}

		IdempotencyKey key = new IdempotencyKey();

		key.setClientId(clientId);
		key.setKey(idempotencyKey);
		key.setTransactionId(
				conversion.getTransactionId()
		);
		key.setCreatedAt(Instant.now());

		idempotencyKeyRepository.save(key);
	}

	private Optional<ConversionResponse> findExistingConversion(
			String clientId,
			String idempotencyKey
	) {
		if (idempotencyKey == null ||
				idempotencyKey.isBlank()) {
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

	private Balance getBalance(
			Map<String, Balance> balancesByCurrency,
			String clientId,
			String currency
	) {
		Balance balance =
				balancesByCurrency.get(currency);

		if (balance == null) {
			throw new BalanceNotFoundException(
					clientId,
					currency
			);
		}

		return balance;
	}
}