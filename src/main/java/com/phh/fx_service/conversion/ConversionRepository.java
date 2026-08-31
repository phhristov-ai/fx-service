package com.phh.fx_service.conversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ConversionRepository
		extends JpaRepository<Conversion, Long>,
		JpaSpecificationExecutor<Conversion> {

	Optional<Conversion> findByTransactionId(UUID transactionId);
}