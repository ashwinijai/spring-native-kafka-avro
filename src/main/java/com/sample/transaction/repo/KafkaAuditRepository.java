package com.sample.transaction.repo;

import com.sample.transaction.entity.KafkaAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KafkaAuditRepository extends JpaRepository<KafkaAudit, Long> {
}
