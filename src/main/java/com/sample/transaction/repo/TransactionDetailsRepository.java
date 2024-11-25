package com.sample.transaction.repo;

import com.sample.transaction.entity.TransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionDetailsRepository extends JpaRepository<TransactionDetails, String> {
    @Query("select distinct(t.customerNo) from TransactionDetails t")
    List<String> getDistinctCustomerNos();
}
