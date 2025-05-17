package com.example.cerbo.dto;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

public class ReportDTO {

    private Long id;

    private LocalDateTime creationDate;

    private String fileName;
    private String path;
}
