package com.recuriq.billing.repository;

import com.recuriq.billing.entity.PriceBookEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PriceBookEntryRepository extends JpaRepository<PriceBookEntry, Long> {
   List<PriceBookEntry> findByPlan_Id(Long planId);
   Optional<PriceBookEntry> findByPlan_IdAndRegionAndCurrency(Long planId, String region, String currency);
}