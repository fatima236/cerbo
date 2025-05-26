package com.example.cerbo.service.reportService;

import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Project;
import com.example.cerbo.entity.Report;
import com.example.cerbo.entity.enums.RemarkStatus;
import com.example.cerbo.entity.enums.ReportStatus;
import com.example.cerbo.exception.ResourceNotFoundException;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.ProjectRepository;
import com.example.cerbo.repository.ReportRepository;
import com.example.cerbo.service.NotificationService;
import com.example.cerbo.service.chatGptService.ChatGptService;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
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
    private final ChatGptService chatGptService;

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

//            String content = docRemarks.stream()
//                    .map(dr -> "- " + dr.getContent())
//                    .collect(Collectors.joining("\n"));

            String prompt = docRemarks.stream()
                    .map(DocumentReview::getContent)
                    .collect(Collectors.joining("\n"));

            String syntheticContent = chatGptService.generateSyntheticRemark(prompt);

            DocumentReview synthetic = new DocumentReview();
            synthetic.setDocument(doc);
            synthetic.setProject(project);
            synthetic.setReport(report);
            synthetic.setContent(syntheticContent);
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

        Path filePath = generateReportPdf(report);
        report.setFilePath(filePath.toString());
        report.setFileName(filePath.getFileName().toString());
        report.setStatus(ReportStatus.SENT);
        report.setSentDate(LocalDateTime.now());

        report.setResponseDeadline(LocalDateTime.now().plusDays(60));

        Project project = report.getProject();
        project.setResponseDeadline(report.getResponseDeadline());
        projectRepository.save(project);

        notificationService.sendNotification(project.getPrincipalInvestigator(),
                "Nouveau rapport disponible",
                "Un nouveau rapport a été ajouté pour le projet \"" + project.getTitle() + "\". Veuillez consulter les remarques et y répondre."
        );

        return reportRepository.save(report);
    }


    public Path generateReportPdf(Report report) throws Exception {

        String fileName = "rapport_" + report.getId() + ".pdf";
        Path folder = Path.of("uploads/reports");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path templatePath = Path.of("src/main/resources/template/Rapport_template.pdf"); // modèle à préparer
        Path outputPath = folder.resolve(fileName);

        try(
                PdfReader reader = new PdfReader(templatePath.toFile());
                PdfWriter writer = new PdfWriter(outputPath.toFile());
                PdfDocument pdfDoc = new PdfDocument(reader, writer);
                Document document = new Document(pdfDoc)
                ){
            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            Map<String, PdfFormField> fields = form.getFormFields();
            Project project = report.getProject();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            fields.get("Text9").setValue("Oujda le "+report.getCreationDate().format(dateFormatter));
            fields.get("Text2").setValue("A Monsieur/Madame le Pr."+project.getPrincipalInvestigator().getFullName());
            fields.get("Text3").setValue("Le Comité d’Ethique pour la Recherche Biomédicale d’Oujda (CERBO) a été saisi le "+project.getSubmissionDate().format(dateFormatter)
            +" d’une demande d’avis concernant votre projet de recherche intitulé <<"+project.getTitle()+">>.");
            fields.get("Text4").setValue("Demande classée sous le N° d’ordre : "+project.getReference());
            fields.get("Text8").setValue("Le comité s’est réuni le "+report.getCreationDate().format(dateFormatter)); // ou date de réunion réelle

            List<DocumentReview> reviews = documentReviewRepository
                    .findByReportIdAndIncludedInReportTrue(report.getId());

            String remarks = reviews.stream()
                    .map(r -> "- " + r.getContent())
                    .collect(Collectors.joining("\n\n"));

            fields.get("Text7").setValue(remarks);

            form.flattenFields();

            document.close();       // si tu utilises `Document`
            pdfDoc.close();         // dans tous les cas


            return outputPath;


        }


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
