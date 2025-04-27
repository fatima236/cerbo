package com.example.cerbo.repository;

import com.example.cerbo.entity.Remark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RemarkRepository extends JpaRepository<Remark,Long> {
    @Query("SELECT r FROM Remark r WHERE r.project.id = :projectId ORDER BY r.creationDate DESC")
    List<Remark> findByProjectIdOrderByCreationDateDesc(Long projectId);

}
