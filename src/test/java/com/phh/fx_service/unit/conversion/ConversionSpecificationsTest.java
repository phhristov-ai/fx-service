package com.phh.fx_service.unit.conversion;

import com.phh.fx_service.conversion.Conversion;
import com.phh.fx_service.conversion.ConversionSpecifications;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionSpecificationsTest {

	@Mock
	private Root<Conversion> root;

	@Mock
	private CriteriaQuery<?> query;

	@Mock
	private CriteriaBuilder criteriaBuilder;

	@Mock
	private Path<Object> transactionIdPath;

	@Test
	void shouldCreateTransactionIdSpecification() {

		UUID transactionId = UUID.randomUUID();

		Predicate predicate = mock(Predicate.class);

		when(root.get("transactionId"))
				.thenReturn(transactionIdPath);

		when(criteriaBuilder.equal(
				transactionIdPath,
				transactionId
		)).thenReturn(predicate);

		Specification<Conversion> specification =
				ConversionSpecifications.hasTransactionId(transactionId);

		Predicate result =
				specification.toPredicate(
						root,
						query,
						criteriaBuilder
				);

		assertSame(predicate, result);

		verify(criteriaBuilder)
				.equal(transactionIdPath, transactionId);
	}

	@Test
	void shouldCreateClientIdSpecification() {

		String clientId = "CLIENT-001";

		Path<Object> clientPath = mock(Path.class);
		Path<Object> clientIdPath = mock(Path.class);

		Predicate predicate = mock(Predicate.class);

		when(root.get("client"))
				.thenReturn(clientPath);

		when(clientPath.get("clientId"))
				.thenReturn(clientIdPath);

		when(criteriaBuilder.equal(
				clientIdPath,
				clientId
		)).thenReturn(predicate);

		Specification<Conversion> specification =
				ConversionSpecifications.hasClientId(clientId);

		Predicate result =
				specification.toPredicate(
						root,
						query,
						criteriaBuilder
				);

		assertSame(predicate, result);

		verify(criteriaBuilder)
				.equal(clientIdPath, clientId);
	}

	@Test
	void shouldCreateDateSpecification() {

		LocalDate date = LocalDate.of(2026, 8, 30);

		Path<Instant> createdAtPath = mock(Path.class);

		Predicate greaterThanOrEqualPredicate = mock(Predicate.class);
		Predicate lessThanPredicate = mock(Predicate.class);
		Predicate combinedPredicate = mock(Predicate.class);

		Instant start =
				date.atStartOfDay(ZoneOffset.UTC)
						.toInstant();

		Instant end =
				date.plusDays(1)
						.atStartOfDay(ZoneOffset.UTC)
						.toInstant();

		when(root.<Instant>get("createdAt"))
				.thenReturn(createdAtPath);

		when(criteriaBuilder.greaterThanOrEqualTo(
				createdAtPath,
				start
		)).thenReturn(greaterThanOrEqualPredicate);

		when(criteriaBuilder.lessThan(
				createdAtPath,
				end
		)).thenReturn(lessThanPredicate);

		when(criteriaBuilder.and(
				greaterThanOrEqualPredicate,
				lessThanPredicate
		)).thenReturn(combinedPredicate);

		Specification<Conversion> specification =
				ConversionSpecifications.createdOn(date);

		Predicate result =
				specification.toPredicate(
						root,
						query,
						criteriaBuilder
				);

		assertSame(combinedPredicate, result);

		verify(criteriaBuilder)
				.greaterThanOrEqualTo(
						createdAtPath,
						start
				);

		verify(criteriaBuilder)
				.lessThan(
						createdAtPath,
						end
				);

		verify(criteriaBuilder)
				.and(
						greaterThanOrEqualPredicate,
						lessThanPredicate
				);
	}
}