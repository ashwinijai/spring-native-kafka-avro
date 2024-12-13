package com.sample.transaction.repo;

import com.sample.transaction.entity.PaymentTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentTypesRepository extends JpaRepository<PaymentTypes, Long> {

    @Query("select p from PaymentTypes p")
    List<PaymentTypes> getPaymentTypes();
}
