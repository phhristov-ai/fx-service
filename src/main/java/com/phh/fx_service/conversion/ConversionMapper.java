package com.phh.fx_service.conversion;

import com.phh.fx_service.balance.BalanceResponse;
import com.phh.fx_service.conversion.dto.ConversionHistoryResponse;
import com.phh.fx_service.conversion.dto.ConversionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversionMapper {

	public ConversionResponse toResponse(
			Conversion conversion,
			List<BalanceResponse> balances
	) {
		return new ConversionResponse(
				conversion.getTransactionId(),
				conversion.getSourceAmount(),
				conversion.getSourceCurrency(),
				conversion.getTargetAmount(),
				conversion.getTargetCurrency(),
				conversion.getRate(),
				conversion.getCreatedAt(),
				balances
		);
	}

	public ConversionHistoryResponse toHistoryResponse(
			Conversion conversion
	) {
		return new ConversionHistoryResponse(
				conversion.getTransactionId(),
				conversion.getSourceAmount(),
				conversion.getSourceCurrency(),
				conversion.getTargetAmount(),
				conversion.getTargetCurrency(),
				conversion.getRate(),
				conversion.getCreatedAt()
		);
	}
}