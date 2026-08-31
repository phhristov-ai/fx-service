package com.phh.fx_service.client;

import com.phh.fx_service.balance.BalanceMapper;
import com.phh.fx_service.balance.BalanceRepository;
import com.phh.fx_service.balance.BalanceResponse;
import com.phh.fx_service.exception.ClientNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientService {

	private final ClientRepository clientRepository;
	private final BalanceRepository balanceRepository;
	private final BalanceMapper balanceMapper;

	@Transactional(readOnly = true)
	public List<BalanceResponse> getBalances(String clientId) {

		clientRepository.findByClientId(clientId)
				.orElseThrow(() ->
						new ClientNotFoundException(clientId));

		return balanceRepository
				.findAllByClientClientId(clientId)
				.stream()
				.map(balanceMapper::toResponse)
				.toList();
	}
}