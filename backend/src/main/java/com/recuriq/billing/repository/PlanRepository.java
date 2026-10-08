package com.recuriq.billing.repository;

import com.recuriq.billing.entity.Plan;
import com.recuriq.billing.enums.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface PlanRepository extends JpaRepository<Plan, Long> {
   List<Plan> findByProduct_Id(Long productId);
   long countByProductId(Long productId);
   List<Plan> findByStatus(Status status);
   List<Plan> findByStatusAndEffectiveToIsNull(Status status);
   List<Plan> findByStatusAndEffectiveToAfterOrEffectiveToIsNull(Status status, LocalDate date);
}
