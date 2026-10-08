package com.recuriq.billing.repository;

import com.recuriq.billing.entity.Customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByUser_Id(Long userId);

	List<Customer> findByUser_FullNameContainingIgnoreCaseOrUser_EmailContainingIgnoreCase(String fullName,
			String email);
}
