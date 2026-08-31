package com.phh.fx_service.unit.client;

import com.phh.fx_service.balance.BalanceResponse;
import com.phh.fx_service.client.ClientController;
import com.phh.fx_service.client.ClientService;
import com.phh.fx_service.exception.ClientNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClientService clientService;

	@Test
	void shouldReturnClientBalances() throws Exception {

		String clientId = "CLIENT-001";

		List<BalanceResponse> balances = List.of(
				new BalanceResponse(
						"USD",
						new BigDecimal("10000.0000")
				),
				new BalanceResponse(
						"EUR",
						new BigDecimal("8000.0000")
				)
		);

		when(clientService.getBalances(clientId))
				.thenReturn(balances);

		mockMvc.perform(
						get("/clients/{clientId}/balances", clientId)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].currency").value("USD"))
				.andExpect(jsonPath("$[0].amount").value(10000.0000))
				.andExpect(jsonPath("$[1].currency").value("EUR"))
				.andExpect(jsonPath("$[1].amount").value(8000.0000));

		verify(clientService)
				.getBalances(clientId);
	}

	@Test
	void shouldReturnNotFoundWhenClientDoesNotExist() throws Exception {

		String clientId = "UNKNOWN";

		when(clientService.getBalances(clientId))
				.thenThrow(new ClientNotFoundException(clientId));

		mockMvc.perform(
						get("/clients/{clientId}/balances", clientId)
				)
				.andExpect(status().isNotFound());
	}
}