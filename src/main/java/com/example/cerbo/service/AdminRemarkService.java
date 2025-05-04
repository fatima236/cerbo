package com.example.cerbo.service;

import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRemarkService {

    private final RemarkRepository remarkRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Remark updateRemarkStatus(Long remarkId, String status, String adminEmail) {
        Remark remark = remarkRepository.findById(remarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Remarque non trouvée"));

        User admin = userRepository.findByEmail(adminEmail);

        remark.setAdminStatus(RemarkStatus.valueOf(status));
        remark.setValidationDate(LocalDateTime.now());
        remark.setValidatedBy(admin);



        return remarkRepository.save(remark);
    }

    public List<Remark> getValidatedRemarks(Long projectId) {
        return remarkRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }
}