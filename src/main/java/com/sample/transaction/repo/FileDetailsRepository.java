package com.sample.transaction.repo;

import com.sample.transaction.entity.FileDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileDetailsRepository extends JpaRepository<FileDetails, String > {
    @Query("select f from FileDetails f where f.createdDate=:createdDate and f.isStagingCompleted=:isProcessed")
    List<FileDetails> findFilesByDate(String createdDate, String isProcessed);

    @Query("select f from FileDetails f where f.isStagingCompleted=:isProcessed")
    List<FileDetails> findUnProcessedFiles(String isProcessed);
}
