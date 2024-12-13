package com.sample.transaction.repo;

import com.sample.transaction.entity.StaticDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StaticDetailsRepository extends JpaRepository<StaticDetails, Long> {

    @Query("select max(s.sNo) from StaticDetails s")
    Long getMaxSno();

    @Query("select s from StaticDetails s where s.processed=:processed")
    List<StaticDetails> getStaticDetails(String processed);
}
