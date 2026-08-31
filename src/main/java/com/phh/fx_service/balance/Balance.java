package com.phh.fx_service.balance;

import com.phh.fx_service.client.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
		name = "balances",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_balance_client_currency",
				columnNames = {"client_id", "currency"}
		)
)
@Getter
@Setter
public class Balance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "client_id", nullable = false)
	private Client client;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false, precision = 19, scale = 4)
	private BigDecimal amount;
}