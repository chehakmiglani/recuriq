package com.recuriq.billing.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.recuriq.billing.entity.DunningRetryLog;
import java.time.LocalDateTime;
import java.util.List;
import com.recuriq.billing.enums.DunningStatus;

public interface DunningRetryLogRepository extends JpaRepository<DunningRetryLog,Long>{
	List<DunningRetryLog> findByStatusAndScheduledAtLessThanEqual(DunningStatus status,LocalDateTime time);
}
