package com.example.cerbo.entity;

import com.example.cerbo.dto.NotificationDTO;
import com.example.cerbo.entity.enums.NotificationStatus;
import com.itextpdf.text.Annotation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String title;

    @Column(nullable = false)
    private String content;

    private LocalDateTime sentDate;

    private String directionUrl;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status = NotificationStatus.NON_LUE;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;


    @PrePersist
    public void prePersist() {
        this.sentDate = LocalDateTime.now();
    }

    public NotificationDTO toDTO() {
        return new NotificationDTO(
                this.id,
                this.title,
                this.content,
                this.sentDate,
                this.status,
                this.directionUrl
        );
    }


}
