package com.example.cerbo.entity;

import com.example.cerbo.entity.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String filePath;

    private LocalDateTime creationDate;

    private LocalDateTime sentDate;

    private LocalDateTime responseDeadline;

    private Boolean responsed = false;

    private LocalDateTime responseDate;

    @Enumerated(EnumType.STRING)

    private ReportStatus status =ReportStatus.NON_ENVOYE;

    @OneToMany(mappedBy = "report")
    private List<DocumentReview> includedReviews;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

}
