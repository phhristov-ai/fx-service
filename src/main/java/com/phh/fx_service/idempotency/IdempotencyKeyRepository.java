package com.phh.fx_service.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository
		extends JpaRepository<IdempotencyKey, Long> {

	Optional<IdempotencyKey> findByClientIdAndKey(
			String clientId,
			String key
	);
}