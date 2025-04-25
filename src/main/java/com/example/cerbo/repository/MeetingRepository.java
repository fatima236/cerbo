package com.example.cerbo.repository;

import com.example.cerbo.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    List<Meeting> findByYear(int year);
    boolean existsByMonthAndYear(String month, int year);
    @Modifying
    @Query("DELETE FROM Meeting m WHERE m.year = :year")
    void deleteByYear(@Param("year") int year);
}