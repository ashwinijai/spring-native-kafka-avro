package com.sample.transaction.repo;

import com.sample.transaction.entity.FileDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileDetailsRepository extends JpaRepository<FileDetails, String > {
    @Query("select f from FileDetails f where f.processed=:processed")
    List<FileDetails> getFileDetails(String processed);

    @Query("select max(s.sNo) from FileDetails s")
    Long getMaxSno();

}
