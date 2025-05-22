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

//        List<DocumentReview> reviews = documentReviewRepository.findByReportIdAndIncludedInReportTrue(report.getId());
//
//        Map<String, List<DocumentReview>> groupedReviews = reviews.stream()
//                .collect(Collectors.groupingBy(review -> review.getDocument().getType().toString()));
//
//        Document document = new Document();
//        PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
//        document.open();
//
//        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLUE);
//        Font remarkDocumentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.LIGHT_GRAY);
//        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.black);
//        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
//        Font redFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.RED);
//
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
//        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//
//        Project project = report.getProject();
//
//        document.add(new Paragraph("Rapport d’évaluation", titleFont));
//        document.add(new Paragraph(" "));
//        document.add(new Paragraph("Projet : " + project.getTitle(), normalFont));
//        document.add(new Paragraph("Référence : " + project.getReference(), normalFont));
//        document.add(new Paragraph("Statut du projet : " + project.getStatus(), normalFont));
//        document.add(new Paragraph("Date de soumission : " + project.getSubmissionDate().format(dateTimeFormatter), normalFont));
//
//        if (project.getResponseDeadline() != null) {
//            document.add(new Paragraph("Date limite de réponse aux remarques : " +
//                    project.getResponseDeadline().format(dateTimeFormatter), redFont));
//        } else {
//            document.add(new Paragraph("Date limite de réponse aux remarques : Non définie", normalFont));
//        }
//
//        document.add(new Paragraph("Date du rapport : " + report.getCreationDate().format(dateFormatter), normalFont));
//        document.add(new Paragraph("Investigateur principal : " + project.getPrincipalInvestigator().getFullName(), normalFont));
//        document.add(new Paragraph(" "));
//
//        document.add(new Paragraph("Remarques incluses :", sectionFont));
//        for (String docType : groupedReviews.keySet()) {
//            document.add(new Paragraph(docType, remarkDocumentFont));
//            List<DocumentReview> docReviews = groupedReviews.get(docType);
//
//            for (DocumentReview review : docReviews) {
//                document.add(new Paragraph( review.getContent(), normalFont));
//            }
//            document.add(new Paragraph(" "));
//        }
//
//        document.add(new Paragraph(" ")); // ligne vide pour espacer
//
//        Font warningFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.RED);
//        document.add(new Paragraph(
//                "AVERTISSEMENT : Vous devez répondre à toutes les remarques disponibles dans votre compte. " +
//                        "Si vous ne répondez pas, le projet sera rejeté automatiquement.",
//                warningFont));
//
//        document.close();
//        return path;
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
