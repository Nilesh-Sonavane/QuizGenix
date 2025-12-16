package com.quizgenix.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.quizgenix.model.Question;
import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;

@Service
public class QuizPdfService {

    // --- PREMIUM COLOR PALETTE ---
    private static final BaseColor HEADER_BG = new BaseColor(30, 41, 59); // #1E293B
    private static final BaseColor ACCENT_BLUE = new BaseColor(59, 130, 246); // #3B82F6
    private static final BaseColor SUCCESS_GREEN = new BaseColor(22, 163, 74); // #16A34A
    private static final BaseColor SUCCESS_BG = new BaseColor(220, 252, 231); // #DCFCE7 (Light Green)
    private static final BaseColor FAIL_RED = new BaseColor(220, 38, 38); // #DC2626
    private static final BaseColor FAIL_BG = new BaseColor(254, 226, 226); // #FEE2E2 (Light Red)
    private static final BaseColor CARD_BORDER = new BaseColor(226, 232, 240); // #E2E8F0
    private static final BaseColor TEXT_MAIN = new BaseColor(15, 23, 42); // #0F172A
    private static final BaseColor TEXT_SUB = new BaseColor(100, 116, 139); // #64748B

    public ByteArrayInputStream generateQuizPdf(Quiz quiz) {
        Document document = new Document(PageSize.A4, 30, 30, 120, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PremiumHeaderFooterEvent());
            document.open();

            // --- FONTS ---
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.WHITE);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_SUB);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_MAIN);
            Font scoreFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36, ACCENT_BLUE);
            Font xpFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(245, 158, 11)); // Orange

            Font questionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_MAIN);
            Font optionFont = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_MAIN);
            Font correctOptionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new BaseColor(21, 128, 61));
            Font wrongOptionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, FAIL_RED);
            Font explanationFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, TEXT_SUB);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_MAIN);

            // 1. HEADER SPACER
            document.add(new Paragraph("\n\n\n"));

            // 2. INFO & SCORE SECTION
            PdfPTable topSection = new PdfPTable(2);
            topSection.setWidthPercentage(100);
            topSection.setWidths(new int[] { 65, 35 });
            topSection.setSpacingAfter(20f);

            // Left Info
            PdfPCell infoCard = new PdfPCell();
            infoCard.setBorder(Rectangle.NO_BORDER);
            infoCard.setPaddingRight(10f);

            PdfPTable infoGrid = new PdfPTable(2);
            infoGrid.setWidthPercentage(100);

            User user = quiz.getUser();
            String fullName = (user != null && user.getFirstName() != null)
                    ? capitalizeWords(user.getFirstName() + " " + user.getLastName())
                    : "Unknown";
            String topic = (quiz.getTopic() != null) ? capitalizeWords(quiz.getTopic()) : "General";
            String date = (quiz.getCreatedAt() != null)
                    ? quiz.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                    : "N/A";

            addInfoField(infoGrid, "CANDIDATE", fullName, labelFont, valueFont);
            addInfoField(infoGrid, "TOPIC", topic, labelFont, valueFont);
            addInfoField(infoGrid, "EMAIL", (user != null) ? user.getEmail() : "", labelFont, valueFont);
            addInfoField(infoGrid, "DATE", date, labelFont, valueFont);

            infoCard.addElement(infoGrid);
            topSection.addCell(infoCard);

            // Right Score
            PdfPCell scoreCard = new PdfPCell();
            scoreCard.setBorder(Rectangle.BOX);
            scoreCard.setBorderColor(CARD_BORDER);
            scoreCard.setBorderWidth(1f);
            scoreCard.setPadding(15f);
            scoreCard.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph scoreLbl = new Paragraph("PERFORMANCE SCORE", labelFont);
            scoreLbl.setAlignment(Element.ALIGN_CENTER);
            scoreCard.addElement(scoreLbl);

            Paragraph scoreVal = new Paragraph(quiz.getScore() + "%", scoreFont);
            scoreVal.setAlignment(Element.ALIGN_CENTER);
            scoreCard.addElement(scoreVal);

            // --- FIXED XP CALCULATION (Matches Thymeleaf Logic) ---
            String diff = (quiz.getDifficulty() != null) ? quiz.getDifficulty() : "Easy";
            int baseXp = 1; // Default

            if (diff.equalsIgnoreCase("Expert"))
                baseXp = 5;
            else if (diff.equalsIgnoreCase("Hard"))
                baseXp = 3;
            else if (diff.equalsIgnoreCase("Medium"))
                baseXp = 2;

            // Calculate correct answers based on score (Same as frontend)
            double correctCount = (quiz.getScore() * quiz.getTotalQuestions()) / 100.0;
            long finalXp = Math.round(correctCount * baseXp);

            Paragraph xpVal = new Paragraph("+" + finalXp + " XP", xpFont);
            // -----------------------------------------------------

            xpVal.setAlignment(Element.ALIGN_CENTER);
            xpVal.setSpacingBefore(2f);
            scoreCard.addElement(xpVal);

            PdfPTable statusBar = new PdfPTable(1);
            statusBar.setWidthPercentage(100);
            PdfPCell barCell = new PdfPCell(new Phrase(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
            barCell.setBackgroundColor(quiz.getScore() >= 50 ? SUCCESS_GREEN : FAIL_RED);
            barCell.setBorder(Rectangle.NO_BORDER);
            barCell.setFixedHeight(4f);
            statusBar.addCell(barCell);

            scoreCard.addElement(new Paragraph("\n"));
            scoreCard.addElement(statusBar);
            topSection.addCell(scoreCard);
            document.add(topSection);

            // 3. STATS ROW
            int totalQ = quiz.getTotalQuestions();
            int correctQ = quiz.getCorrectAnswers();
            int wrongQ = totalQ - correctQ;

            PdfPTable statsRow = new PdfPTable(3);
            statsRow.setWidthPercentage(100);
            statsRow.setSpacingAfter(25f);
            statsRow.setWidths(new int[] { 33, 33, 33 });

            statsRow.addCell(createMetricCard("Total Questions", String.valueOf(totalQ), TEXT_MAIN));
            statsRow.addCell(createMetricCard("Correct", String.valueOf(correctQ), SUCCESS_GREEN));
            statsRow.addCell(createMetricCard("Incorrect", String.valueOf(wrongQ), FAIL_RED));

            document.add(statsRow);

            // 4. QUESTIONS LIST
            Paragraph solHeader = new Paragraph("DETAILED SOLUTIONS", sectionFont);
            solHeader.setSpacingAfter(10f);
            document.add(solHeader);

            List<Question> questions = quiz.getQuestions();
            if (questions != null) {
                int count = 1;
                for (Question q : questions) {

                    PdfPTable qCard = new PdfPTable(1);
                    qCard.setWidthPercentage(100);
                    qCard.setSpacingBefore(15f);

                    PdfPCell qCell = new PdfPCell();
                    qCell.setBorderColor(CARD_BORDER);
                    qCell.setBorderWidth(1f);
                    qCell.setPadding(0);

                    // A. Question Text
                    PdfPTable titleTable = new PdfPTable(1);
                    titleTable.setWidthPercentage(100);
                    PdfPCell titleCell = new PdfPCell(new Phrase("Q" + count + ". " + q.getText(), questionFont));
                    titleCell.setBackgroundColor(new BaseColor(248, 250, 252));
                    titleCell.setBorder(Rectangle.BOTTOM);
                    titleCell.setBorderColor(CARD_BORDER);
                    titleCell.setPadding(10f);
                    titleTable.addCell(titleCell);
                    qCell.addElement(titleTable);

                    // B. Options
                    PdfPTable optTable = new PdfPTable(1);
                    optTable.setWidthPercentage(100);

                    List<String> options = q.getOptions();

                    String dbCorrectText = "";
                    int correctIdx = q.getCorrectAnswerIndex();
                    if (correctIdx != -1 && options != null && correctIdx < options.size()) {
                        dbCorrectText = options.get(correctIdx);
                    } else if (q.getCorrectAnswer() != null) {
                        dbCorrectText = q.getCorrectAnswer();
                    }

                    String userSelectedText = (q.getUserAnswer() != null) ? q.getUserAnswer().trim() : "";

                    if (options != null) {
                        for (int i = 0; i < options.size(); i++) {
                            String optText = options.get(i);
                            char letter = (char) ('A' + i);

                            boolean isCorrectOption = optText.equalsIgnoreCase(dbCorrectText);
                            boolean isUserSelected = optText.equalsIgnoreCase(userSelectedText);

                            PdfPCell optCell = new PdfPCell();
                            optCell.setBorder(Rectangle.NO_BORDER);
                            optCell.setPaddingTop(4f);
                            optCell.setPaddingBottom(4f);
                            optCell.setPaddingLeft(10f);

                            if (isCorrectOption) {
                                optCell.setBackgroundColor(SUCCESS_BG);
                                optCell.addElement(
                                        new Paragraph(letter + ". " + optText + "  [ Correct ]", correctOptionFont));
                            } else if (isUserSelected && !isCorrectOption) {
                                optCell.setBackgroundColor(FAIL_BG);
                                optCell.addElement(
                                        new Paragraph(letter + ". " + optText + "  [ Your Answer ]", wrongOptionFont));
                            } else {
                                optCell.addElement(new Paragraph(letter + ". " + optText, optionFont));
                            }
                            optTable.addCell(optCell);
                        }
                    }
                    optTable.setSpacingBefore(5f);
                    optTable.setSpacingAfter(5f);
                    qCell.addElement(optTable);

                    // C. Explanation
                    String expText = (q.getExplanation() != null && !q.getExplanation().isEmpty()) ? q.getExplanation()
                            : null;
                    if (expText != null) {
                        PdfPTable expWrapper = new PdfPTable(1);
                        expWrapper.setWidthPercentage(100);

                        PdfPCell expCell = new PdfPCell();
                        expCell.setBorder(Rectangle.TOP);
                        expCell.setBorderColor(CARD_BORDER);
                        expCell.setPadding(8f);
                        expCell.addElement(new Paragraph("💡 Explanation: " + expText, explanationFont));

                        expWrapper.addCell(expCell);
                        qCell.addElement(expWrapper);
                    }

                    qCard.addCell(qCell);
                    document.add(qCard);

                    count++;
                }
            }

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // --- HELPERS ---

    private void addInfoField(PdfPTable table, String label, String value, Font lblF, Font valF) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(8f);
        cell.addElement(new Paragraph(label, lblF));
        cell.addElement(new Paragraph(value, valF));
        table.addCell(cell);
    }

    private PdfPCell createMetricCard(String label, String value, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(CARD_BORDER);
        cell.setPadding(10f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPTable topBar = new PdfPTable(1);
        topBar.setWidthPercentage(100);
        PdfPCell bar = new PdfPCell(new Phrase(" "));
        bar.setBackgroundColor(color);
        bar.setFixedHeight(3f);
        bar.setBorder(Rectangle.NO_BORDER);
        topBar.addCell(bar);
        cell.addElement(topBar);

        Font valFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, color);
        Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXT_SUB);

        Paragraph v = new Paragraph(value, valFont);
        v.setAlignment(Element.ALIGN_CENTER);
        v.setSpacingBefore(5f);
        cell.addElement(v);

        Paragraph l = new Paragraph(label, lblFont);
        l.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(l);

        return cell;
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty())
            return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    // --- HEADER EVENT ---
    class PremiumHeaderFooterEvent extends PdfPageEventHelper {
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BaseColor.WHITE);
        Font watermarkFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 60, new BaseColor(230, 230, 230, 50));
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_SUB);

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            canvas.setColorFill(HEADER_BG);
            canvas.rectangle(0, document.getPageSize().getHeight() - 100, document.getPageSize().getWidth(), 100);
            canvas.fill();

            try {
                Image logo = Image.getInstance(new ClassPathResource("static/images/logo.png").getURL());
                logo.scaleToFit(45, 45);
                logo.setAbsolutePosition(40, document.getPageSize().getHeight() - 70);
                canvas.addImage(logo);

                ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                        new Phrase("QuizNest", brandFont), 95, document.getPageSize().getHeight() - 60, 0);
            } catch (Exception e) {
                ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                        new Phrase("QN QuizNest", brandFont), 40, document.getPageSize().getHeight() - 60, 0);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContentUnder();
            canvas.saveState();
            PdfGState gs1 = new PdfGState();
            gs1.setFillOpacity(0.1f);
            canvas.setGState(gs1);

            float x = (document.getPageSize().getLeft() + document.getPageSize().getRight()) / 2;
            float y = (document.getPageSize().getTop() + document.getPageSize().getBottom()) / 2;

            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase("GearNest", watermarkFont), x, y, 45);

            canvas.restoreState();

            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Page " + writer.getPageNumber() + " | Generated by QuizNest", footerFont),
                    x, 20, 0);
        }
    }
}