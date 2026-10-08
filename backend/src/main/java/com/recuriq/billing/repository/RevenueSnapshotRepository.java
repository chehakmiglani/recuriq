package com.recuriq.billing.repository;

import com.recuriq.billing.entity.RevenueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueSnapshotRepository extends JpaRepository<RevenueSnapshot, Long> {

    Optional<RevenueSnapshot> findBySnapshotDate(LocalDate snapshotDate);

    List<RevenueSnapshot> findBySnapshotDateBetweenOrderBySnapshotDateAsc(LocalDate startDate, LocalDate endDate);

    List<RevenueSnapshot> findAllByOrderBySnapshotDateAsc();

    Page<RevenueSnapshot> findAllByOrderBySnapshotDateAsc(Pageable pageable);

    Page<RevenueSnapshot> findAllByOrderByIdDesc(Pageable pageable);
}
