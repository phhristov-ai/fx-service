package com.phh.fx_service.conversion;

import com.phh.fx_service.client.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversions")
@Getter
@Setter
public class Conversion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_id", nullable = false, unique = true)
	private UUID transactionId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Column(name = "source_currency", nullable = false, length = 3)
	private String sourceCurrency;

	@Column(name = "source_amount", nullable = false, precision = 19, scale = 4)
	private BigDecimal sourceAmount;

	@Column(name = "target_currency", nullable = false, length = 3)
	private String targetCurrency;

	@Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
	private BigDecimal targetAmount;

	@Column(nullable = false, precision = 19, scale = 10)
	private BigDecimal rate;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(
			name = "source_balance_after",
			nullable = false,
			precision = 19,
			scale = 4
	)
	private BigDecimal sourceBalanceAfter;

	@Column(
			name = "target_balance_after",
			nullable = false,
			precision = 19,
			scale = 4
	)
	private BigDecimal targetBalanceAfter;
}