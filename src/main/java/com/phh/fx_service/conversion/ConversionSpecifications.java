package com.phh.fx_service.conversion;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

public class ConversionSpecifications {
 private ConversionSpecifications() {
   /* This utility class should not be instantiated */
 }


	public static Specification<Conversion> hasTransactionId(
			UUID transactionId
	) {
		return (root, query, cb) ->
				cb.equal(root.get("transactionId"), transactionId);
	}

	public static Specification<Conversion> hasClientId(
			String clientId
	) {
		return (root, query, cb) ->
				cb.equal(
						root.get("client").get("clientId"),
						clientId
				);
	}

	public static Specification<Conversion> createdOn(
			LocalDate date
	) {
		Instant start =
				date.atStartOfDay(ZoneOffset.UTC).toInstant();

		Instant end =
				date.plusDays(1)
						.atStartOfDay(ZoneOffset.UTC)
						.toInstant();

		return (root, query, cb) ->
				cb.and(
						cb.greaterThanOrEqualTo(
								root.get("createdAt"),
								start
						),
						cb.lessThan(
								root.get("createdAt"),
								end
						)
				);
	}
}