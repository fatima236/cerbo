package com.example.cerbo.service;

import com.example.cerbo.entity.Project;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

@Service
public class AvisFavorableService {

    private static final Logger logger = LoggerFactory.getLogger(AvisFavorableService.class);

    public Path generateAvisFavorable(Project project) throws Exception {
        String fileName = "avis_favorable_" + project.getReference() + ".pdf";
        Path folder = Path.of("uploads/avis_favorable");

        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path templatePath = Path.of("src/main/resources/template/avis_favorable.pdf");
        Path outputPath = folder.resolve(fileName);

        try (PdfReader reader = new PdfReader(templatePath.toFile());
             PdfWriter writer = new PdfWriter(outputPath.toFile());
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            Map<String, PdfFormField> fields = form.getFormFields();

            // Remplir les champs avec les NOMS SIMPLIFIÉS
            setFieldIfExists(fields, "dhFormfield-5699153895", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            setFieldIfExists(fields, "dhFormfield-5699153788", project.getReference());
            setFieldIfExists(fields, "dhFormfield-5699153789", project.getTitle());
            setFieldIfExists(fields, "dhFormfield-5699153790", "Pr. " + project.getPrincipalInvestigator().getFullName());
            setFieldIfExists(fields, "dhFormfield-5699153791", project.getFundingSource());

            setFieldIfExists(fields, "dhFormfield-5699153793", project.getStudyDuration());

            form.flattenFields();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération de l'avis favorable", e);
            throw e;
        }

        return outputPath;
    }

    private void setFieldIfExists(Map<String, PdfFormField> fields, String fieldName, String value) {
        PdfFormField field = fields.get(fieldName);
        if (field != null) {
            field.setValue(value);
        } else {
            logger.warn("Champ PDF non trouvé: {}", fieldName);
            // Option: créer le champ manquant si nécessaire
        }
    }
}