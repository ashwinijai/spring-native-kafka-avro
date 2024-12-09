package com.sample.transaction.repo;

import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Component
public class CustomRepository {
    @PersistenceContext
    private EntityManager em;
    public List<Object[]> executeQueryFromCache(String query) {
        return em.createNativeQuery(query).getResultList();
    }
}
