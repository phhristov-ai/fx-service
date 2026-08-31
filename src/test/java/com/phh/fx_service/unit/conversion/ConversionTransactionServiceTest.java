package com.phh.fx_service.unit.conversion;

import com.phh.fx_service.balance.Balance;
import com.phh.fx_service.balance.BalanceMapper;
import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.balance.BalanceResponse;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import com.phh.fx_service.conversion.Conversion;
import com.phh.fx_service.conversion.ConversionMapper;
import com.phh.fx_service.conversion.ConversionRepository;
import com.phh.fx_service.conversion.ConversionTransactionService;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import com.phh.fx_service.exception.BalanceNotFoundException;
import com.phh.fx_service.exception.ClientNotFoundException;
import com.phh.fx_service.exception.InsufficientFundsException;
import com.phh.fx_service.idempotency.IdempotencyKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionTransactionServiceTest {

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private ConversionRepository conversionRepository;

	@Mock
	private IdempotencyKeyRepository idempotencyKeyRepository;

	@Mock
	private ConversionMapper conversionMapper;

	@Mock
	private BalanceMapper balanceMapper;

	@InjectMocks
	private ConversionTransactionService transactionService;

	@Test
	void shouldConvertSuccessfully() {

		String clientId = "CLIENT-001";

		BigDecimal sourceAmount =
				new BigDecimal("100.0000");

		BigDecimal rate =
				new BigDecimal("0.8500");

		Client client = new Client();
		client.setClientId(clientId);

		Balance usdBalance = new Balance();
		usdBalance.setCurrency("USD");
		usdBalance.setAmount(new BigDecimal("10000.0000"));
		usdBalance.setClient(client);

		Balance eurBalance = new Balance();
		eurBalance.setCurrency("EUR");
		eurBalance.setAmount(new BigDecimal("8000.0000"));
		eurBalance.setClient(client);

		when(clientRepository.findByClientId(clientId))
				.thenReturn(Optional.of(client));

		when(balanceRepository.findForUpdate(
				eq(clientId),
				eq(List.of("USD", "EUR"))
		)).thenReturn(List.of(
				usdBalance,
				eurBalance
		));

		Conversion savedConversion = new Conversion();
		savedConversion.setTransactionId(UUID.randomUUID());
		savedConversion.setClient(client);
		savedConversion.setSourceCurrency("USD");
		savedConversion.setSourceAmount(sourceAmount);
		savedConversion.setTargetCurrency("EUR");
		savedConversion.setTargetAmount(
				new BigDecimal("85.0000")
		);
		savedConversion.setRate(rate);
		savedConversion.setCreatedAt(Instant.now());
		savedConversion.setSourceBalanceAfter(
				new BigDecimal("9900.0000")
		);
		savedConversion.setTargetBalanceAfter(
				new BigDecimal("8085.0000")
		);

		when(conversionRepository.save(any(Conversion.class)))
				.thenReturn(savedConversion);

		BalanceResponse usdResponse =
				new BalanceResponse(
						"USD",
						new BigDecimal("9900.0000")
				);

		BalanceResponse eurResponse =
				new BalanceResponse(
						"EUR",
						new BigDecimal("8085.0000")
				);

		when(balanceRepository.findAllByClientClientId(clientId))
				.thenReturn(List.of(
						usdBalance,
						eurBalance
				));

		when(balanceMapper.toResponse(usdBalance))
				.thenReturn(usdResponse);

		when(balanceMapper.toResponse(eurBalance))
				.thenReturn(eurResponse);

		ConversionResponse expectedResponse =
				mock(ConversionResponse.class);

		when(conversionMapper.toResponse(
				same(savedConversion),
				anyList()
		)).thenReturn(expectedResponse);

		ConversionResponse result =
				transactionService.execute(
						clientId,
						null,
						"USD",
						"EUR",
						sourceAmount,
						rate
				);

		assertSame(expectedResponse, result);

		assertThat(usdBalance.getAmount())
				.isEqualByComparingTo("9900.0000");

		assertThat(eurBalance.getAmount())
				.isEqualByComparingTo("8085.0000");

		ArgumentCaptor<Conversion> captor =
				ArgumentCaptor.forClass(Conversion.class);

		verify(conversionRepository)
				.save(captor.capture());

		Conversion conversion =
				captor.getValue();

		assertThat(conversion.getTransactionId())
				.isNotNull();

		assertThat(conversion.getClient())
				.isSameAs(client);

		assertThat(conversion.getSourceCurrency())
				.isEqualTo("USD");

		assertThat(conversion.getSourceAmount())
				.isEqualByComparingTo("100.0000");

		assertThat(conversion.getTargetCurrency())
				.isEqualTo("EUR");

		assertThat(conversion.getTargetAmount())
				.isEqualByComparingTo("85.0000");

		assertThat(conversion.getRate())
				.isEqualByComparingTo("0.8500");

		assertThat(conversion.getSourceBalanceAfter())
				.isEqualByComparingTo("9900.0000");

		assertThat(conversion.getTargetBalanceAfter())
				.isEqualByComparingTo("8085.0000");

		assertThat(conversion.getCreatedAt())
				.isNotNull();

		verify(balanceRepository)
				.findForUpdate(
						clientId,
						List.of("USD", "EUR")
				);

		verify(conversionMapper)
				.toResponse(
						same(savedConversion),
						anyList()
				);
	}

	@Test
	void shouldRejectUnknownClient() {

		String clientId = "CLIENT-999";

		when(clientRepository.findByClientId(clientId))
				.thenReturn(Optional.empty());

		assertThrows(
				ClientNotFoundException.class,
				() -> transactionService.execute(
						clientId,
						null,
						"USD",
						"EUR",
						new BigDecimal("100.00"),
						new BigDecimal("0.8500")
				)
		);

		verify(balanceRepository, never())
				.findForUpdate(anyString(), anyList());

		verify(conversionRepository, never())
				.save(any());
	}

	@Test
	void shouldRejectMissingSourceBalance() {

		String clientId = "CLIENT-001";

		Client client = new Client();
		client.setClientId(clientId);

		when(clientRepository.findByClientId(clientId))
				.thenReturn(Optional.of(client));

		Balance eurBalance = new Balance();
		eurBalance.setCurrency("EUR");
		eurBalance.setAmount(new BigDecimal("8000.00"));

		when(balanceRepository.findForUpdate(
				eq(clientId),
				eq(List.of("GBP", "EUR"))
		)).thenReturn(List.of(eurBalance));

		assertThrows(
				BalanceNotFoundException.class,
				() -> transactionService.execute(
						clientId,
						null,
						"GBP",
						"EUR",
						new BigDecimal("100.0000"),
						new BigDecimal("1.1700")
				)
		);

		verify(conversionRepository, never())
				.save(any());
	}

	@Test
	void shouldRejectConversionWhenFundsAreInsufficient() {

		String clientId = "CLIENT-001";

		BigDecimal sourceAmount =
				new BigDecimal("10001.0000");

		BigDecimal rate =
				new BigDecimal("0.8500");

		Client client = new Client();
		client.setClientId(clientId);

		Balance usdBalance = new Balance();
		usdBalance.setCurrency("USD");
		usdBalance.setAmount(new BigDecimal("10000.0000"));
		usdBalance.setClient(client);

		Balance eurBalance = new Balance();
		eurBalance.setCurrency("EUR");
		eurBalance.setAmount(new BigDecimal("8000.0000"));
		eurBalance.setClient(client);

		when(clientRepository.findByClientId(clientId))
				.thenReturn(Optional.of(client));

		when(balanceRepository.findForUpdate(
				eq(clientId),
				eq(List.of("USD", "EUR"))
		)).thenReturn(List.of(
				usdBalance,
				eurBalance
		));

		assertThrows(
				InsufficientFundsException.class,
				() -> transactionService.execute(
						clientId,
						null,
						"USD",
						"EUR",
						sourceAmount,
						rate
				)
		);

		assertThat(usdBalance.getAmount())
				.isEqualByComparingTo("10000.0000");

		assertThat(eurBalance.getAmount())
				.isEqualByComparingTo("8000.0000");

		verify(conversionRepository, never())
				.save(any());

		verify(idempotencyKeyRepository, never())
				.save(any());

		verify(conversionMapper, never())
				.toResponse(any(), any());
	}

}
