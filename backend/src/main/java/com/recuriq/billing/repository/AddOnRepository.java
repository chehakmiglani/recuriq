package com.recuriq.billing.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recuriq.billing.entity.AddOn;
import com.recuriq.billing.enums.Status;

public interface AddOnRepository extends JpaRepository<AddOn, Long> {
	   List<AddOn> findByStatus(Status status);
}