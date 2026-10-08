package com.recuriq.billing.repository;

import com.recuriq.billing.entity.MeteredComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MeteredComponentRepository extends JpaRepository<MeteredComponent, Long> {
   List<MeteredComponent> findByPlan_Id(Long planId);
}