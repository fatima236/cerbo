package com.example.cerbo.service;

import com.example.cerbo.entity.Project;
import com.example.cerbo.repository.UserRepository;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.PdfFormField;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.cerbo.entity.User;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class AvisFavorableService {

    private static final Logger logger = LoggerFactory.getLogger(AvisFavorableService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    @Autowired
    private UserRepository userRepository;
    public Path generateAvisFavorable(Project project) throws Exception {
        String fileName = "avis_favorable_" + project.getReference() + ".pdf";
        Path folder = Path.of("uploads/avis_favorable");
        // Récupérer un admin (le premier trouvé)
        User admin = userRepository.findFirstByRolesContaining("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Aucun administrateur trouvé"));
        if (!Files.exists(folder)) {
            Files.createDirectories(folder);
        }

        Path templatePath = Path.of("src/main/resources/template/avis_favorable_templt.pdf");
        Path outputPath = folder.resolve(fileName);

        try (PdfReader reader = new PdfReader(templatePath.toFile());
             PdfWriter writer = new PdfWriter(outputPath.toFile());
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, true);
            Map<String, PdfFormField> fields = form.getFormFields();
            String submissionDate = project.getSubmissionDate().format(DATE_FORMATTER);

            // Nouveau code pour la date du jour
            String currentDate = LocalDateTime.now().format(DATE_FORMATTER);
            setFieldIfExists(fields, "Text3", currentDate); // 🔵 Nom du champ à vérifier
            // Remplir les champs avec les NOMS SIMPLIFIÉS
            setFieldIfExists(fields, "dhFormfield-5699363529", project.getReference());
            setFieldIfExists(fields, "dhFormfield-5699364360", project.getTitle());
            setFieldIfExists(fields, "dhFormfield-5699364362", project.getPrincipalInvestigator().getFullName());
            setFieldIfExists(fields, "dhFormfield-5699364366", project.getFundingSource());
            setFieldIfExists(fields, "dhFormfield-5699364391", submissionDate);
            setFieldIfExists(fields, "dhFormfield-5699364395", project.getStudyDuration());
            // Ajout du promoteur (admin)
            setFieldIfExists(fields, "NOM_DU_CHAMP_PROMOTEUR", admin.getFullName());

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