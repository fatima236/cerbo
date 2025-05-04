package com.example.cerbo.controller;

import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.service.AdminRemarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/remarks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRemarkController {

    private final AdminRemarkService adminRemarkService;

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
}