package com.phh.fx_service.unit.conversion;

import com.phh.fx_service.balance.BalanceMapper;
import com.phh.fx_service.conversion.*;
import com.phh.fx_service.conversion.dto.ConversionHistoryResponse;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import com.phh.fx_service.conversion.dto.CreateConversionRequest;
import com.phh.fx_service.exception.*;
import com.phh.fx_service.idempotency.IdempotencyKey;
import com.phh.fx_service.idempotency.IdempotencyKeyRepository;
import com.phh.fx_service.validation.CurrencyValidator;
import com.phh.fx_service.rate.RateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

	@Mock
	private ConversionRepository conversionRepository;

	@Mock
	private IdempotencyKeyRepository idempotencyKeyRepository;

	@Mock
	private RateService rateService;

	@Mock
	private ConversionMapper conversionMapper;

	@Mock
	private BalanceMapper balanceMapper;

	@Mock
	private CurrencyValidator currencyValidator;

	@Mock
	private ConversionTransactionService transactionService;

	@InjectMocks
	private ConversionService conversionService;

	@Test
	void shouldGetRateAndDelegateConversion() {

		String clientId = "CLIENT-001";

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"EUR",
						new BigDecimal("100.00")
				);

		BigDecimal rate = new BigDecimal("0.8500");

		ConversionResponse expectedResponse =
				mock(ConversionResponse.class);

		when(rateService.getRate("USD", "EUR"))
				.thenReturn(rate);

		when(transactionService.execute(
				eq(clientId),
				isNull(),
				eq("USD"),
				eq("EUR"),
				eq(new BigDecimal("100.0000")),
				eq(rate)
		)).thenReturn(expectedResponse);

		ConversionResponse result =
				conversionService.convert(
						clientId,
						null,
						request
				);

		assertSame(expectedResponse, result);

		verify(rateService)
				.getRate("USD", "EUR");

		verify(transactionService)
				.execute(
						clientId,
						null,
						"USD",
						"EUR",
						new BigDecimal("100.0000"),
						rate
				);

		verifyNoInteractions(
				conversionRepository,
				conversionMapper,
				balanceMapper
		);
	}

	@Test
	void shouldReturnExistingConversionForIdempotencyKey() {

		String clientId = "CLIENT-001";
		String idempotencyKey = "abc123";

		UUID transactionId = UUID.randomUUID();

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"EUR",
						new BigDecimal("100.00")
				);

		IdempotencyKey existingKey = new IdempotencyKey();
		existingKey.setClientId(clientId);
		existingKey.setKey(idempotencyKey);
		existingKey.setTransactionId(transactionId);

		Conversion existingConversion = new Conversion();
		existingConversion.setTransactionId(transactionId);

		ConversionResponse expectedResponse =
				mock(ConversionResponse.class);

		when(idempotencyKeyRepository.findByClientIdAndKey(
				clientId,
				idempotencyKey
		)).thenReturn(Optional.of(existingKey));

		when(conversionRepository.findByTransactionId(transactionId))
				.thenReturn(Optional.of(existingConversion));

		when(conversionMapper.toResponse(
				same(existingConversion),
				anyList()
		)).thenReturn(expectedResponse);

		ConversionResponse result =
				conversionService.convert(
						clientId,
						idempotencyKey,
						request
				);

		assertSame(expectedResponse, result);

		verify(rateService, never())
				.getRate(anyString(), anyString());

		verify(conversionRepository, never())
				.save(any());

		verify(idempotencyKeyRepository, never())
				.save(any());
	}

	@Test
	void shouldRejectNullAmount() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"EUR",
						null
				);

		assertThrows(
				InvalidAmountException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectZeroAmount() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"EUR",
						BigDecimal.ZERO
				);

		assertThrows(
				InvalidAmountException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectNegativeAmount() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"EUR",
						new BigDecimal("-10.00")
				);

		assertThrows(
				InvalidAmountException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectAmountWithMoreThanFourDecimalPlaces() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"EUR",
						new BigDecimal("100.12345")
				);

		assertThrows(
				InvalidAmountException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectInvalidSourceCurrency() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"XXX",
						"EUR",
						new BigDecimal("100.00")
				);

		doThrow(new InvalidCurrencyException("XXX"))
				.when(currencyValidator)
				.validate("XXX");

		assertThrows(
				InvalidCurrencyException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectInvalidTargetCurrency() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"XXX",
						new BigDecimal("100.00")
				);

		doNothing()
				.when(currencyValidator)
				.validate("USD");

		doThrow(new InvalidCurrencyException("XXX"))
				.when(currencyValidator)
				.validate("XXX");

		assertThrows(
				InvalidCurrencyException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectBlankSourceCurrency() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						" ",
						"EUR",
						new BigDecimal("100.00")
				);

		assertThrows(
				InvalidCurrencyException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectBlankTargetCurrency() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						" ",
						new BigDecimal("100.00")
				);

		assertThrows(
				InvalidCurrencyException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(
				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldRejectSameSourceAndTargetCurrency() {

		CreateConversionRequest request =
				new CreateConversionRequest(
						"USD",
						"USD",
						new BigDecimal("100.00")
				);

		assertThrows(
				InvalidCurrencyException.class,
				() -> conversionService.convert(
						"CLIENT-001",
						null,
						request
				)
		);

		verifyNoInteractions(

				rateService,
				conversionRepository
		);
	}

	@Test
	void shouldFindConversionsByClientId() {

		String clientId = "CLIENT-001";

		Pageable pageable = PageRequest.of(0, 20);

		Conversion conversion = new Conversion();

		ConversionHistoryResponse response =
				new ConversionHistoryResponse(
						UUID.randomUUID(),
						new BigDecimal("100.00"),
						"USD",
						new BigDecimal("85.00"),
						"EUR",
						new BigDecimal("0.8500"),
						Instant.now()
				);

		Page<Conversion> conversions =
				new PageImpl<>(
						List.of(conversion),
						pageable,
						1
				);

		when(conversionRepository.findAll(
				any(Specification.class),
				eq(pageable)
		)).thenReturn(conversions);

		when(conversionMapper.toHistoryResponse(conversion))
				.thenReturn(response);

		Page<ConversionHistoryResponse> result =
				conversionService.findConversions(
						null,
						null,
						clientId,
						pageable
				);

		assertThat(result.getContent())
				.containsExactly(response);

		assertThat(result.getTotalElements())
				.isEqualTo(1);

		verify(conversionRepository)
				.findAll(
						any(Specification.class),
						eq(pageable)
				);

		verify(conversionMapper)
				.toHistoryResponse(conversion);
	}

	@Test
	void shouldFindConversionsByTransactionId() {

		UUID transactionId = UUID.randomUUID();

		Pageable pageable = PageRequest.of(0, 20);

		Conversion conversion = new Conversion();

		ConversionHistoryResponse response =
				mock(ConversionHistoryResponse.class);

		when(conversionRepository.findAll(
				any(Specification.class),
				eq(pageable)
		)).thenReturn(
				new PageImpl<>(
						List.of(conversion),
						pageable,
						1
				)
		);

		when(conversionMapper.toHistoryResponse(conversion))
				.thenReturn(response);

		Page<ConversionHistoryResponse> result =
				conversionService.findConversions(
						transactionId,
						null,
						null,
						pageable
				);

		assertThat(result.getContent())
				.containsExactly(response);

		verify(conversionRepository)
				.findAll(
						any(Specification.class),
						eq(pageable)
				);
	}

	@Test
	void shouldFindConversionsByDate() {

		LocalDate date =
				LocalDate.of(2026, 8, 30);

		Pageable pageable =
				PageRequest.of(0, 20);

		when(conversionRepository.findAll(
				any(Specification.class),
				eq(pageable)
		)).thenReturn(
				new PageImpl<>(
						List.of(),
						pageable,
						0
				)
		);

		Page<ConversionHistoryResponse> result =
				conversionService.findConversions(
						null,
						date,
						null,
						pageable
				);

		assertThat(result.getContent())
				.isEmpty();

		verify(conversionRepository)
				.findAll(
						any(Specification.class),
						eq(pageable)
				);
	}

	@Test
	void shouldCombineMultipleFilters() {

		UUID transactionId = UUID.randomUUID();

		LocalDate date =
				LocalDate.of(2026, 8, 30);

		String clientId = "CLIENT-001";

		Pageable pageable =
				PageRequest.of(1, 10);

		when(conversionRepository.findAll(
				any(Specification.class),
				eq(pageable)
		)).thenReturn(
				new PageImpl<>(
						List.of(),
						pageable,
						0
				)
		);

		Page<ConversionHistoryResponse> result =
				conversionService.findConversions(
						transactionId,
						date,
						clientId,
						pageable
				);

		assertThat(result.getContent())
				.isEmpty();

		verify(conversionRepository)
				.findAll(
						any(Specification.class),
						eq(pageable)
				);
	}

	@Test
	void shouldRejectConversionHistoryWithoutFilters() {

		Pageable pageable =
				PageRequest.of(0, 20);

		assertThrows(
				InvalidConversionHistoryFilterException.class,
				() -> conversionService.findConversions(
						null,
						null,
						null,
						pageable
				)
		);

		verifyNoInteractions(
				conversionRepository,
				conversionMapper
		);
	}

	@Test
	void shouldRejectConversionHistoryWithBlankClientId() {

		Pageable pageable =
				PageRequest.of(0, 20);

		assertThrows(
				InvalidConversionHistoryFilterException.class,
				() -> conversionService.findConversions(
						null,
						null,
						"   ",
						pageable
				)
		);

		verifyNoInteractions(
				conversionRepository,
				conversionMapper
		);
	}

	@Test
	void shouldPassPageableToRepository() {

		Pageable pageable =
				PageRequest.of(2, 10);

		when(conversionRepository.findAll(
				any(Specification.class),
				eq(pageable)
		)).thenReturn(
				new PageImpl<>(
						List.of(),
						pageable,
						25
				)
		);

		Page<ConversionHistoryResponse> result =
				conversionService.findConversions(
						null,
						null,
						"CLIENT-001",
						pageable
				);

		assertThat(result.getNumber())
				.isEqualTo(2);

		assertThat(result.getSize())
				.isEqualTo(10);

		assertThat(result.getTotalElements())
				.isEqualTo(25);

		verify(conversionRepository)
				.findAll(
						any(Specification.class),
						eq(pageable)
				);
	}
}