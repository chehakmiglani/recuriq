package com.recuriq.billing.repository;

import com.recuriq.billing.entity.DailyRevenueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyRevenueSnapshotRepository extends JpaRepository<DailyRevenueSnapshot, Long> {
    Optional<DailyRevenueSnapshot> findBySnapshotDate(LocalDate snapshotDate);
    List<DailyRevenueSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate startDate, LocalDate endDate);
}
