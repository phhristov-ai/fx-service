package com.phh.fx_service.unit.client;

import com.phh.fx_service.balance.*;
import com.phh.fx_service.client.Client;
import com.phh.fx_service.client.ClientRepository;
import com.phh.fx_service.client.ClientService;
import com.phh.fx_service.exception.ClientNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

	@Mock
	private ClientRepository clientRepository;

	@Mock
	private BalanceRepository balanceRepository;

	@Mock
	private BalanceMapper balanceMapper;

	@InjectMocks
	private ClientService clientService;

	@Test
	void shouldReturnClientBalances() {

		String clientId = "CLIENT-001";

		Client client = new Client();
		client.setClientId(clientId);

		Balance usdBalance = new Balance();
		usdBalance.setClient(client);
		usdBalance.setCurrency("USD");
		usdBalance.setAmount(new BigDecimal("10000.00"));

		Balance eurBalance = new Balance();
		eurBalance.setClient(client);
		eurBalance.setCurrency("EUR");
		eurBalance.setAmount(new BigDecimal("8000.00"));

		BalanceResponse usdResponse =
				new BalanceResponse(
						"USD",
						new BigDecimal("10000.00")
				);

		BalanceResponse eurResponse =
				new BalanceResponse(
						"EUR",
						new BigDecimal("8000.00")
				);

		when(clientRepository.findByClientId(clientId))
				.thenReturn(Optional.of(client));

		when(balanceRepository.findAllByClientClientId(clientId))
				.thenReturn(List.of(usdBalance, eurBalance));

		when(balanceMapper.toResponse(usdBalance))
				.thenReturn(usdResponse);

		when(balanceMapper.toResponse(eurBalance))
				.thenReturn(eurResponse);

		List<BalanceResponse> result =
				clientService.getBalances(clientId);

		assertThat(result)
				.containsExactly(
						usdResponse,
						eurResponse
				);

		verify(clientRepository)
				.findByClientId(clientId);

		verify(balanceRepository)
				.findAllByClientClientId(clientId);

		verify(balanceMapper)
				.toResponse(usdBalance);

		verify(balanceMapper)
				.toResponse(eurBalance);
	}

	@Test
	void shouldRejectUnknownClient() {

		String clientId = "UNKNOWN";

		when(clientRepository.findByClientId(clientId))
				.thenReturn(Optional.empty());

		assertThrows(
				ClientNotFoundException.class,
				() -> clientService.getBalances(clientId)
		);

		verify(clientRepository)
				.findByClientId(clientId);

		verifyNoInteractions(
				balanceRepository,
				balanceMapper
		);
	}
}
