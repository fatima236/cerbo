package com.example.cerbo.service;

import com.example.cerbo.dto.meeting.MeetingMinutesDTO;
import com.example.cerbo.entity.Meeting;
import com.example.cerbo.entity.User;
import com.example.cerbo.repository.DocumentReviewRepository;
import com.example.cerbo.repository.MeetingAttendanceRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;

@Service
public class PdfGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(PdfGenerationService.class);

    private final DocumentReviewRepository documentReviewRepository;
    private final MeetingAttendanceRepository meetingAttendanceRepository;

    // Injection via constructeur
    public PdfGenerationService(DocumentReviewRepository documentReviewRepository,
                                MeetingAttendanceRepository meetingAttendanceRepository) {
        this.documentReviewRepository = documentReviewRepository;
        this.meetingAttendanceRepository = meetingAttendanceRepository;
    }
    public byte[] generateMinutesPdf(MeetingMinutesDTO minutes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        // Ajouter le gestionnaire d'événements pour header/footer
        pdf.addEventHandler(PdfDocumentEvent.START_PAGE, new HeaderFooterHandler());

        Document document = new Document(pdf);
        // Ajustez ces marges selon la taille de vos images
        document.setMargins(100, 36, 70, 36); // Haut:100px, Bas:70px // Haut:100px (header), Bas:80px (footer)
        try {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);

          /*  // === HEADER ===
            Image headerImg = new Image(ImageDataFactory.create(getClass().getResource("/static/header.png")));
            headerImg.setWidth(UnitValue.createPercentValue(100));
            headerImg.setFixedPosition(0, pdf.getDefaultPageSize().getTop() - 30); // Position fixe en haut
            document.add(headerImg);*/


            // 1. En-tête du PV (identique au template)
            Paragraph title = new Paragraph("PV DE RÉUNION CERBO")
                    .setFont(font)
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            // 2. Session et Date (formatées comme dans le template)
            document.add(new Paragraph("Session: " + minutes.getSession()).setFont(font));
            document.add(new Paragraph("Date de réunion: " +
                    minutes.getMeetingDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .setFont(font));

            // 3. Membres présents (liste avec tirets comme dans le template)
            document.add(new Paragraph("\nMembres CERBO présent:").setFont(font).setBold());
            for (MeetingMinutesDTO.AttendeeInfo member : minutes.getPresentMembers()) {
                document.add(new Paragraph("- " + member.getFullName()).setFont(font));
            }

            // 4. Membres examinateurs (liste avec tirets)
            document.add(new Paragraph("\nMembres CERBO examinateur (remarques soumises sur plateforme CERBO):")
                    .setFont(font).setBold());
            for (MeetingMinutesDTO.AttendeeInfo examiner : minutes.getExaminers()) {
                document.add(new Paragraph("- " + examiner.getFullName()).setFont(font));
            }

            // 5. Nouveaux Projets examinés (tableau identique au template)
            document.add(new Paragraph("\nNouveaux Projets examinés:").setFont(font).setBold());
            float[] columnWidths = {2, 4, 3, 2};
            Table projectsTable = new Table(columnWidths);
            projectsTable.setWidth(UnitValue.createPercentValue(100));

            // En-têtes du tableau
            projectsTable.addHeaderCell(createHeaderCell(font, "Référence_CERBO"));
            projectsTable.addHeaderCell(createHeaderCell(font, "Intitulé"));
            projectsTable.addHeaderCell(createHeaderCell(font, "Investigateur Principal"));
            projectsTable.addHeaderCell(createHeaderCell(font, "Décision"));

            // Données des projets
            for (MeetingMinutesDTO.ProjectReview project : minutes.getReviewedProjects()) {
                projectsTable.addCell(createCell(font, project.getReference()));
                projectsTable.addCell(createCell(font, project.getTitle()));
                projectsTable.addCell(createCell(font, project.getPrincipalInvestigator()));
                projectsTable.addCell(createCell(font, project.getDecision() != null ? project.getDecision() : ""));
            }
            document.add(projectsTable);

            // 6. Réponses investigateurs (tableau identique au template)
            document.add(new Paragraph("\nRéponses investigateurs:").setFont(font).setBold());
            Table responsesTable = new Table(columnWidths);
            responsesTable.setWidth(UnitValue.createPercentValue(100));

            // En-têtes
            responsesTable.addHeaderCell(createHeaderCell(font, "Référence_CERBO"));
            responsesTable.addHeaderCell(createHeaderCell(font, "Intitulé"));
            responsesTable.addHeaderCell(createHeaderCell(font, "Investigateur Principal"));
            responsesTable.addHeaderCell(createHeaderCell(font, "Décision"));

            // Données
            for (MeetingMinutesDTO.InvestigatorResponse response : minutes.getInvestigatorResponses()) {
                responsesTable.addCell(createCell(font, response.getReference()));
                responsesTable.addCell(createCell(font, response.getTitle()));
                responsesTable.addCell(createCell(font, response.getPrincipalInvestigator()));
                responsesTable.addCell(createCell(font, response.getDecision() != null ? response.getDecision() : ""));
            }
            document.add(responsesTable);

         /*   // === FOOTER ===
            Image footerImg = new Image(ImageDataFactory.create(getClass().getResource("/static/footer.png")));
            footerImg.setWidth(UnitValue.createPercentValue(100));

            // Calculer la position Y pour le footer (bas de page + marge)
            float footerY = pdf.getDefaultPageSize().getBottom() + 20;
            footerImg.setFixedPosition(0, footerY);
            document.add(footerImg);*/

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Erreur lors de la génération du PDF", e);
            throw e;
        } finally {
            // Fermeture des ressources
            if (document != null) document.close();
            if (pdf != null) pdf.close();
            if (writer != null) writer.close();
        }
    }

    // Méthodes utilitaires pour créer les cellules (inchangées)
    private Cell createHeaderCell(PdfFont font, String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setBold())
                .setBackgroundColor(new DeviceRgb(220, 220, 220))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createCell(PdfFont font, String text) {
        return new Cell()
                .add(new Paragraph(text).setFont(font))
                .setPadding(5);
    }



    private List<MeetingMinutesDTO.AttendeeInfo> getExaminers(Meeting meeting) {
        // Récupérer les évaluateurs avec des soumissions finales
        List<User> finalExaminers = documentReviewRepository.findFinalExaminersByMeetingId(meeting.getId());

        return finalExaminers.stream()
                .map(user -> {
                    MeetingMinutesDTO.AttendeeInfo info = new MeetingMinutesDTO.AttendeeInfo();
                    info.setUserId(user.getId());
                    info.setFullName(user.getFullName());
                    info.setRole("Examinateur CERBO");
                    info.setManual(false);
                    return info;
                })
                .collect(Collectors.toList());
    }

    private class HeaderFooterHandler implements IEventHandler {
        @Override
        public void handleEvent(Event event) {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
            PdfPage page = docEvent.getPage();
            Rectangle pageSize = page.getPageSize();
            float marginLeft = 36; // Marge gauche de 36px (1 inch)
            float marginRight = 36; // Marge droite de 36px

            try {
                // === HEADER ===
                Image headerImg = new Image(ImageDataFactory.create(getClass().getResource("/static/header.png")));

                // Ajuster la largeur pour tenir compte des marges
                float headerWidth = pageSize.getWidth() - marginLeft - marginRight;
                headerImg.setWidth(headerWidth);

                // Calculer la hauteur proportionnelle
                float headerHeight = headerImg.getImageHeight() * (headerWidth / headerImg.getImageWidth());

                // Position Y: haut de page moins la hauteur de l'image
                float headerY = pageSize.getTop() - headerHeight;

                // Dessiner le header
                new Canvas(new PdfCanvas(page), pageSize)
                        .add(headerImg.setFixedPosition(marginLeft, headerY, headerWidth))
                        .close();

                // === FOOTER ===
                Image footerImg = new Image(ImageDataFactory.create(getClass().getResource("/static/footer.png")));

                // Ajuster la largeur pour tenir compte des marges
                float footerWidth = pageSize.getWidth() - marginLeft - marginRight;
                footerImg.setWidth(footerWidth);

                // Calculer la hauteur proportionnelle
                float footerHeight = footerImg.getImageHeight() * (footerWidth / footerImg.getImageWidth());

                // Position Y: bas de page
                float footerY = pageSize.getBottom();

                // Dessiner le footer
                new Canvas(new PdfCanvas(page), pageSize)
                        .add(footerImg.setFixedPosition(marginLeft, footerY, footerWidth))
                        .close();

            } catch (Exception e) {
                logger.error("Erreur lors du chargement des images header/footer", e);
            }
        }
    }}