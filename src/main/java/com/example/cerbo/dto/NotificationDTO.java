package com.example.cerbo.dto;

import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.NotificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import com.example.cerbo.entity.enums.ProjectStatus;
import java.time.LocalDateTime;

import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@AllArgsConstructor @NoArgsConstructor
public class NotificationDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime sentDate;
    private NotificationStatus status;
    private String directionUrl;
}