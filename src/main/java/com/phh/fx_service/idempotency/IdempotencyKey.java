package com.phh.fx_service.idempotency;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "idempotency_keys",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_idempotency_client_key",
				columnNames = {"client_id", "idempotency_key"}
		)
)
@Getter
@Setter
public class IdempotencyKey {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "client_id", nullable = false, length = 100)
	private String clientId;

	@Column(name = "idempotency_key", nullable = false, length = 255)
	private String key;

	@Column(name = "transaction_id", nullable = false)
	private UUID transactionId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}