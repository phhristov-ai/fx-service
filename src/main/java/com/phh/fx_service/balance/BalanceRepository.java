package com.phh.fx_service.balance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<Balance, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
        select b
        from Balance b
        where b.client.clientId = :clientId
          and b.currency in :currencies
        order by b.currency
        """)
	List<Balance> findForUpdate(
			@Param("clientId") String clientId,
			@Param("currencies") Collection<String> currencies
	);

	List<Balance> findAllByClientClientId(String clientId);

	Optional<Balance> findByClientClientIdAndCurrency(
			String clientId,
			String currency
	);
}