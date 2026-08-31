package com.phh.fx_service.unit.rate;

import com.phh.fx_service.exception.RateProviderException;
import com.phh.fx_service.rate.RateController;
import com.phh.fx_service.rate.RateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateController.class)
class RateControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RateService rateService;

	@Test
	void shouldReturnExchangeRate() throws Exception {

		when(rateService.getRate("USD", "EUR"))
				.thenReturn(new BigDecimal("0.8500"));

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.from").value("USD"))
				.andExpect(jsonPath("$.to").value("EUR"))
				.andExpect(jsonPath("$.rate").value(0.8500));

		verify(rateService)
				.getRate("USD", "EUR");
	}

	@Test
	void shouldReturnUppercaseCurrencies() throws Exception {

		when(rateService.getRate("usd", "eur"))
				.thenReturn(new BigDecimal("0.8500"));

		mockMvc.perform(
						get("/rates")
								.param("from", "usd")
								.param("to", "eur")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.from").value("USD"))
				.andExpect(jsonPath("$.to").value("EUR"));

		verify(rateService)
				.getRate("usd", "eur");
	}

	@Test
	void shouldReturnErrorWhenProviderFails() throws Exception {

		when(rateService.getRate("USD", "EUR"))
				.thenThrow(
						new RateProviderException(
								"Provider unavailable"
						)
				);

		mockMvc.perform(
						get("/rates")
								.param("from", "USD")
								.param("to", "EUR")
				)
				.andExpect(status().is5xxServerError());
	}
}