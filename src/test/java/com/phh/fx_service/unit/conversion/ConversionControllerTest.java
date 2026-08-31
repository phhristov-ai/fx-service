package com.phh.fx_service.unit.conversion;

import com.phh.fx_service.conversion.ConversionController;
import com.phh.fx_service.conversion.ConversionService;
import com.phh.fx_service.conversion.dto.ConversionHistoryResponse;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import com.phh.fx_service.conversion.dto.CreateConversionRequest;
import com.phh.fx_service.exception.InvalidConversionHistoryFilterException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ConversionController.class)
class ConversionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ConversionService conversionService;

	@Test
	void shouldConvertSuccessfully() throws Exception {

		ConversionResponse response =
				mock(ConversionResponse.class);

		when(conversionService.convert(
				eq("CLIENT-001"),
				eq("test-key"),
				any(CreateConversionRequest.class)
		)).thenReturn(response);

		mockMvc.perform(
						post("/conversions")
								.header("X-Client-Id", "CLIENT-001")
								.header("Idempotency-Key", "test-key")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                              {
                                "sourceCurrency": "USD",
                                "targetCurrency": "EUR",
                                "amount": 100.00
                              }
                              """)
				)
				.andExpect(status().isOk());

		verify(conversionService)
				.convert(
						eq("CLIENT-001"),
						eq("test-key"),
						any(CreateConversionRequest.class)
				);
	}

	@Test
	void shouldRejectRequestWithoutClientId() throws Exception {

		mockMvc.perform(
						post("/conversions")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                          {
                            "sourceCurrency": "USD",
                            "targetCurrency": "EUR",
                            "amount": 100.00
                          }
                          """)
				)
				.andExpect(status().isBadRequest());

		verifyNoInteractions(conversionService);
	}

	@Test
	void shouldRejectInvalidRequest() throws Exception {

		mockMvc.perform(
						post("/conversions")
								.header("X-Client-Id", "CLIENT-001")
								.contentType(MediaType.APPLICATION_JSON)
								.content("""
                          {
                            "sourceCurrency": "",
                            "targetCurrency": "EUR",
                            "amount": -100
                          }
                          """)
				)
				.andExpect(status().isBadRequest());

		verifyNoInteractions(conversionService);
	}

	@Test
	void shouldFindConversionsByClientId() throws Exception {

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

		Page<ConversionHistoryResponse> page =
				new PageImpl<>(
						List.of(response),
						PageRequest.of(0, 20),
						1
				);

		when(conversionService.findConversions(
				eq(null),
				eq(null),
				eq("CLIENT-001"),
				any(Pageable.class)
		)).thenReturn(page);

		mockMvc.perform(
						get("/conversions")
								.param("clientId", "CLIENT-001")
								.param("page", "0")
								.param("size", "20")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(
						jsonPath("$.content[0].sourceCurrency")
								.value("USD")
				)
				.andExpect(
						jsonPath("$.content[0].targetCurrency")
								.value("EUR")
				);

		verify(conversionService).findConversions(
				eq(null),
				eq(null),
				eq("CLIENT-001"),
				any(Pageable.class)
		);
	}

	@Test
	void shouldFindConversionsByTransactionId() throws Exception {

		UUID transactionId = UUID.randomUUID();

		Page<ConversionHistoryResponse> page =
				new PageImpl<>(List.of());

		when(conversionService.findConversions(
				eq(transactionId),
				eq(null),
				eq(null),
				any(Pageable.class)
		)).thenReturn(page);

		mockMvc.perform(
						get("/conversions")
								.param(
										"transactionId",
										transactionId.toString()
								)
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());

		verify(conversionService).findConversions(
				eq(transactionId),
				eq(null),
				eq(null),
				any(Pageable.class)
		);
	}

	@Test
	void shouldFindConversionsByDate() throws Exception {

		LocalDate date = LocalDate.of(2026, 8, 30);

		Page<ConversionHistoryResponse> page =
				new PageImpl<>(List.of());

		when(conversionService.findConversions(
				eq(null),
				eq(date),
				eq(null),
				any(Pageable.class)
		)).thenReturn(page);

		mockMvc.perform(
						get("/conversions")
								.param("date", "2026-08-30")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty());

		verify(conversionService).findConversions(
				eq(null),
				eq(date),
				eq(null),
				any(Pageable.class)
		);
	}

	@Test
	void shouldPassAllFiltersAndPaginationToService() throws Exception {

		UUID transactionId = UUID.randomUUID();
		LocalDate date = LocalDate.of(2026, 8, 30);

		Page<ConversionHistoryResponse> page =
				new PageImpl<>(
						List.of(),
						PageRequest.of(2, 10),
						25
				);

		when(conversionService.findConversions(
				eq(transactionId),
				eq(date),
				eq("CLIENT-001"),
				any(Pageable.class)
		)).thenReturn(page);

		mockMvc.perform(
						get("/conversions")
								.param(
										"transactionId",
										transactionId.toString()
								)
								.param("date", "2026-08-30")
								.param("clientId", "CLIENT-001")
								.param("page", "2")
								.param("size", "10")
				)
				.andExpect(status().isOk());

		ArgumentCaptor<Pageable> pageableCaptor =
				ArgumentCaptor.forClass(Pageable.class);

		verify(conversionService).findConversions(
				eq(transactionId),
				eq(date),
				eq("CLIENT-001"),
				pageableCaptor.capture()
		);

		Pageable pageable = pageableCaptor.getValue();

		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(10);
	}

	@Test
	void shouldRejectRequestWithoutFilters() throws Exception {

		when(conversionService.findConversions(
				isNull(),
				isNull(),
				isNull(),
				any(Pageable.class)
		)).thenThrow(
				new InvalidConversionHistoryFilterException()
		);

		mockMvc.perform(get("/conversions"))
				.andExpect(status().isBadRequest())
				.andExpect(
						jsonPath("$.code")
								.value("INVALID_CONVERSION_HISTORY_FILTER")
				);
	}
}