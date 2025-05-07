package com.example.cerbo.controller;

import com.example.cerbo.entity.Remark;
import com.example.cerbo.repository.RemarkRepository;
import com.example.cerbo.service.AdminRemarkService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.cerbo.dto.ReportPreview;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/remarks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRemarkController {

    private final AdminRemarkService adminRemarkService;
    private final RemarkRepository remarkRepository;


    @GetMapping("/pending")
    public ResponseEntity<List<Remark>> getPendingRemarks() {
        List<Remark> remarks = adminRemarkService.getPendingRemarks();
        return ResponseEntity.ok(remarks);
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<Remark>> getProjectRemarks(@PathVariable Long projectId) {
        List<Remark> remarks = remarkRepository.findByProjectIdOrderByCreationDateDesc(projectId);
        return ResponseEntity.ok(remarks);
    }

    @PutMapping("/{remarkId}")
    public ResponseEntity<Remark> updateRemark(
            @PathVariable Long remarkId,
            @RequestBody UpdateRemarkRequest request) {

        Remark updatedRemark = adminRemarkService.updateRemarkContent(
                remarkId,
                request.getContent(),
                request.getComment()
        );
        return ResponseEntity.ok(updatedRemark);
    }

    @PutMapping("/{remarkId}/status")
    public ResponseEntity<Remark> updateRemarkStatus(
            @PathVariable Long remarkId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String status = request.get("status");
        String adminEmail = authentication.getName();

        Remark updatedRemark = adminRemarkService.updateRemarkStatus(remarkId, status, adminEmail);
        return ResponseEntity.ok(updatedRemark);
    }

    @GetMapping("/projects/{projectId}/validated")
    public ResponseEntity<List<Remark>> getValidatedRemarks(@PathVariable Long projectId) {
        List<Remark> remarks = adminRemarkService.getValidatedRemarks(projectId);
        return ResponseEntity.ok(remarks);
    }

    @PostMapping("/projects/{projectId}/generate-report")
    public ResponseEntity<ReportPreview> generateReportPreview(
            @PathVariable Long projectId,
            @RequestBody List<Long> remarkIds) {

        ReportPreview preview = adminRemarkService.generateReportPreview(projectId, remarkIds);
        return ResponseEntity.ok(preview);
    }
}

@Data
class UpdateRemarkRequest {
    private String content;
    private String comment;
}

