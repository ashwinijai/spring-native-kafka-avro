package com.sample.transaction.repo;

import com.sample.transaction.entity.FileDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileDetailsRepository extends JpaRepository<FileDetails, String > {
    @Query("select f from FileDetails f where f.extractDate=:extractDate and f.processed=:isProcessed")
    List<FileDetails> findFilesByDate(String extractDate, String isProcessed);

    @Query("select f from FileDetails f where f.processed=:isProcessed")
    List<FileDetails> findUnProcessedFiles(String isProcessed);
    @Query("select max(s.sNo) from FileDetails s")
    Long getMaxSno();

}
