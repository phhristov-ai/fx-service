package com.phh.fx_service.client;

import com.phh.fx_service.balance.BalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class ClientController {

	private final ClientService clientService;

	@GetMapping("/{clientId}/balances")
	public List<BalanceResponse> getBalances(
			@PathVariable String clientId
	) {
		return clientService.getBalances(clientId);
	}
}