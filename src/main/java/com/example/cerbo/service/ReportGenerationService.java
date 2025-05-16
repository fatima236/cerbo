package com.example.cerbo.service;

import com.example.cerbo.entity.DocumentReview;
import com.example.cerbo.entity.Remark;
import com.example.cerbo.entity.Report;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class ReportGenerationService {
    DocumentReviewRepository documentReviewRepository;

    public Path generateReportPdf(Report report) throws Exception {

        String fileName = "rapport_" + report.getId() + ".pdf";
        Path folder = Path.of("uploads/reports");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path path = folder.resolve(fileName);

        List<DocumentReview> reviews = documentReviewRepository.findValidatedRemarksByProjectId(report.getProject().getId());

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
                document.add(new Paragraph("remarque " + remarkNumber + ": " + review.getRemark()));
                remarkNumber++;
            }
            document.add(new Paragraph(" "));
        }

        document.close();
        return path;
    }
}
