package com.example.cerbo.service.reportService;

import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Report;
import com.example.cerbo.entity.User;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.ReportRepository;
import com.example.cerbo.service.NotificationService;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final DocumentReviewRepository documentReviewRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    @Transactional
    public Report createReport(Long projectId, List<Long> reviewIds) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        Report report = new Report();
        report.setProject(project);
        report.setCreationDate(LocalDateTime.now());
        report.setResponseDeadline(LocalDateTime.now().plusDays(7));
        reportRepository.save(report);

        List<DocumentReview> reviews = documentReviewRepository.findAllById(reviewIds);

        Map<com.example.cerbo.entity.Document, List<DocumentReview>> grouped = reviews.stream()
                .collect(Collectors.groupingBy(DocumentReview::getDocument));

        // Vérifier que toutes les remarques appartiennent au même projet
        reviews.forEach(review -> {
            if (!review.getDocument().getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Review does not belong to the specified project");
            }

            if (review.getStatus() != RemarkStatus.VALIDATED) {
                throw new IllegalArgumentException("Only validated reviews can be included in a report");
            }
        });

        for (Map.Entry<com.example.cerbo.entity.Document, List<DocumentReview>> entry : grouped.entrySet()) {
            com.example.cerbo.entity.Document doc = entry.getKey();
            List<DocumentReview> docRemarks = entry.getValue();

            String content = docRemarks.stream()
                    .map(dr -> "- " + dr.getContent())
                    .collect(Collectors.joining("\n"));

            DocumentReview synthetic = new DocumentReview();
            synthetic.setDocument(doc);
            synthetic.setProject(project);
            synthetic.setReport(report);
            synthetic.setContent(content);
            synthetic.setIncludedInReport(true);
            synthetic.setFinalized(true);
            synthetic.setFinal_submission(true);
            synthetic.setStatus(RemarkStatus.VALIDATED);
            synthetic.setCreationDate(LocalDateTime.now());

            documentReviewRepository.save(synthetic);
        }

        projectRepository.save(project);

        report.setStatus(ReportStatus.NON_ENVOYE);

        Report savedReport = reportRepository.save(report);

        return savedReport;
    }

    @Transactional
    public Report finalizeAndSendReport(Long reportId) throws Exception {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        // Générer le PDF
        Path filePath = generateReportPdf(report);
        report.setFilePath(filePath.toString());
        report.setFileName(filePath.getFileName().toString());
        report.setStatus(ReportStatus.SENT);
        report.setSentDate(LocalDateTime.now());

        // Définir une date limite de réponse (7 jours par exemple)
        report.setResponseDeadline(LocalDateTime.now().plusDays(7));

        // Mettre à jour le projet
        Project project = report.getProject();
        project.setResponseDeadline(report.getResponseDeadline());
        projectRepository.save(project);

        // Notifier l'investigateur principal
//        User investigator = project.getPrincipalInvestigator();
//        notificationService.notifyUser(
//                investigator.getId(),
//                "Nouveau rapport disponible",
//                "Un rapport concernant votre projet est disponible. Veuillez y répondre avant le " +
//                        report.getResponseDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
//        );

        return reportRepository.save(report);
    }


    public Path generateReportPdf(Report report) throws Exception {

        String fileName = "rapport_" + report.getId() + ".pdf";
        Path folder = Path.of("uploads/reports");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path path = folder.resolve(fileName);

        List<DocumentReview> reviews = documentReviewRepository.findByReportIdAndIncludedInReportTrue(report.getId());

        Map<String, List<DocumentReview>> groupedReviews = reviews.stream()
                .collect(Collectors.groupingBy(review -> review.getDocument().getType().toString()));

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(path.toFile()));
        document.open();


        document.add(new Paragraph("Rapport d'évaluation"));
        document.add(new Paragraph("Projet : " + report.getProject().getTitle()));
        document.add(new Paragraph("Date : " + report.getCreationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Remarques incluses :", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));

        for (String docType : groupedReviews.keySet()) {
            document.add(new Paragraph(docType, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            List<DocumentReview> docReviews = groupedReviews.get(docType);
            int remarkNumber = 1;
            for (DocumentReview review : docReviews) {
                document.add(new Paragraph("remarque " + remarkNumber + ": " + review.getContent()));
                remarkNumber++;
            }
            document.add(new Paragraph(" "));
        }

        document.close();
        return path;
    }
    @Transactional(readOnly = true)
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    @Transactional(readOnly = true)
    public List<Report> getReportsByProjectId(Long projectId) {
        return reportRepository.findByProjectId(projectId);
    }
}
