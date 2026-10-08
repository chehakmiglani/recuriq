package com.recuriq.billing.repository;

import com.recuriq.billing.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {
	@Query("SELECT t FROM TaxRate t WHERE t.region = :region AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date)")
	Optional<TaxRate> findByRegionAndEffectiveToIsNullOrFuture(@Param("region") String region, @Param("date") LocalDate date);

	@Query("SELECT COUNT(t) FROM TaxRate t WHERE t.effectiveTo IS NULL OR t.effectiveTo >= CURRENT_DATE")
	long countActiveTaxRates();
}