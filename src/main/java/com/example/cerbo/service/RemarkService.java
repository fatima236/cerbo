package com.example.cerbo.service;

import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemarkService {

    private final RemarkRepository remarkRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public Remark addRemark(Long projectId, String content, String evaluatorEmail) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User evaluator = userRepository.findByEmail(evaluatorEmail);

        Remark remark = new Remark();
        remark.setContent(content);
        remark.setProject(project);
        remark.setReviewer(evaluator);
        remark.setCreationDate(LocalDateTime.now());
        remark.setAdminStatus(RemarkStatus.PENDING);

        return remarkRepository.save(remark);
    }

    public List<Remark> getValidatedRemarks(Long projectId) {
        return remarkRepository.findByProjectIdAndAdminStatus(projectId, RemarkStatus.VALIDATED);
    }
}