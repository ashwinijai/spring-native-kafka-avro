package com.sample.transaction.repo;

import com.sample.transaction.entity.StaticDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticDetailsRepository extends JpaRepository<StaticDetails, Long> {

    @Query("select max(s.sNo) from StaticDetails s")
    Long getMaxSno();
}
