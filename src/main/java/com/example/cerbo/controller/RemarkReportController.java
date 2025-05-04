package com.example.cerbo.controller;

import com.example.cerbo.service.RemarkReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/projects/{projectId}/report")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RemarkReportController {

    private final RemarkReportService remarkReportService;

    @PostMapping("/send")
    public ResponseEntity<?> sendReportToInvestigator(@PathVariable Long projectId) {
        try {
            remarkReportService.generateAndSendReport(projectId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rapport envoyé avec succès à l'investigateur principal"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}