package com.example.cerbo.repository;

import com.example.cerbo.entity.Training;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingRepository extends JpaRepository<Training,Long> {
}
