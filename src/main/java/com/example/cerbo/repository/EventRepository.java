package com.example.cerbo.repository;

import com.example.cerbo.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event,Long> {
    List<Event> findTop3ByOrderByStartDateDesc();

}
