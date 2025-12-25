package com.quizgenix.service;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.quizgenix.dto.SystemEventDTO;
import com.quizgenix.model.Payment;
import com.quizgenix.model.Quiz;
import com.quizgenix.model.User;
import com.quizgenix.repository.PaymentRepository;
import com.quizgenix.repository.QuizRepository;
import com.quizgenix.repository.UserRepository;

@Service
public class ReportService {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuizRepository quizRepository;

    // --- COLORS & FONTS ---
    private static final BaseColor BRAND_COLOR = new BaseColor(139, 92, 246);
    private static final BaseColor HEADER_BG = new BaseColor(241, 245, 249);
    private static final BaseColor TEXT_DARK = new BaseColor(30, 41, 59);

    private static final Font FONT_BRAND = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND_COLOR);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, TEXT_DARK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_DARK);
    private static final Font FONT_SUMMARY_VAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_DARK);

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return "Unknown";
        }

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                // Capitalize first letter, append the rest
                result.append(Character.toUpperCase(word.charAt(0)));
                result.append(word.substring(1));

                // Add space between words, but not after the last one
                if (i < words.length - 1) {
                    result.append(" ");
                }
            }
        }
        return result.toString();
    }

    // --- 1. CORE DATA FETCHING (With Date Filter Support) ---
    public List<SystemEventDTO> getSystemEvents(LocalDate startDate, LocalDate endDate) {
        List<SystemEventDTO> events = new ArrayList<>();

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : LocalDateTime.MIN;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.MAX;

        List<Payment> payments = paymentRepository.findAll();
        for (Payment p : payments) {
            String desc = "Payment: " + formatCurrency(p.getAmount()) + " (" + p.getPlanName() + ")";
            String status = isSuccessful(p.getStatus()) ? "Success" : "Failed";
            events.add(new SystemEventDTO("Revenue", desc, p.getCreatedAt(), status));
        }

        List<User> users = userRepository.findAll();
        for (User u : users) {
            events.add(new SystemEventDTO("System", "New User: " + u.getEmail(), u.getCreatedAt(), "Success"));
        }

        List<Quiz> quizzes = quizRepository.findAll();
        for (Quiz q : quizzes) {
            String topicName = toTitleCase(q.getTopic());
            events.add(
                    new SystemEventDTO("Activity", "Quiz: " + topicName, q.getCreatedAt(), "Success"));
        }

        return events.stream()
                .filter(e -> e.getDate() != null)
                .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                .sorted(Comparator.comparing(SystemEventDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public List<SystemEventDTO> getRecentSystemEvents() {
        return getSystemEvents(null, null).stream().limit(100).collect(Collectors.toList());
    }

    // --- 2. GENERATE PDF (With Filter) ---
    public byte[] generatePdfReport(LocalDate startDate, LocalDate endDate) {
        try {
            Document document = new Document(PageSize.A4, 36, 36, 36, 50);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooterPageEvent());

            document.open();

            // Stats Calculation
            List<SystemEventDTO> logs = getSystemEvents(startDate, endDate);
            double totalRevenue = logs.stream()
                    .filter(l -> l.getType().equals("Revenue") && l.getStatus().equals("Success"))
                    .mapToDouble(l -> parseAmountFromDesc(l.getDescription()))
                    .sum();

            long totalUsers = logs.stream().filter(l -> l.getType().equals("System")).count();
            long totalQuizzes = logs.stream().filter(l -> l.getType().equals("Activity")).count();

            // --- HEADER START ---
            PdfPTable titleTable = new PdfPTable(2);
            titleTable.setWidthPercentage(100);
            // FIX: Changed from {1f, 4f} to {3f, 4f} to give "GearNest" enough space
            titleTable.setWidths(new float[] { 3f, 4f });

            // Left Side: Logo + "GearNest"
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPTable brandInner = new PdfPTable(2);
            brandInner.setWidthPercentage(100);
            brandInner.setWidths(new float[] { 1f, 3f });

            PdfPCell imgCell = new PdfPCell();
            imgCell.setBorder(Rectangle.NO_BORDER);
            imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                Image logo = Image.getInstance(new ClassPathResource("static/images/logo.png").getURL());
                logo.scaleToFit(35, 35);
                imgCell.addElement(logo);
            } catch (Exception e) {
                imgCell.addElement(new Phrase("", FONT_BRAND));
            }
            brandInner.addCell(imgCell);

            PdfPCell brandTextCell = new PdfPCell();
            brandTextCell.setBorder(Rectangle.NO_BORDER);
            brandTextCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            Paragraph appName = new Paragraph("GearNest", FONT_BRAND);
            appName.setLeading(0, 1.2f);
            brandTextCell.addElement(appName);
            brandInner.addCell(brandTextCell);

            logoCell.addElement(brandInner);
            titleTable.addCell(logoCell);

            // Right Side: Report Title & Date
            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            textCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph title = new Paragraph("SYSTEM ANALYTICS REPORT", FONT_TITLE);
            title.setAlignment(Element.ALIGN_RIGHT);
            textCell.addElement(title);

            String dateRange = (startDate != null && endDate != null) ? startDate + " to " + endDate : "All Time";
            DateTimeFormatter headerFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
            Paragraph date = new Paragraph(
                    "Generated: " + LocalDateTime.now().format(headerFmt) + "\nPeriod: " + dateRange, FONT_SUBTITLE);
            date.setAlignment(Element.ALIGN_RIGHT);
            textCell.addElement(date);

            titleTable.addCell(textCell);
            document.add(titleTable);
            // --- HEADER END ---

            document.add(new Paragraph("\n"));

            // Summary Cards
            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20f);
            summaryTable.setWidths(new float[] { 1f, 1f, 1f });

            addSummaryCard(summaryTable, "Revenue", formatCurrency(totalRevenue),
                    "https://img.icons8.com/fluency/48/money-bag.png");
            addSummaryCard(summaryTable, "New Users", String.valueOf(totalUsers),
                    "https://img.icons8.com/fluency/48/group.png");
            addSummaryCard(summaryTable, "Quizzes", String.valueOf(totalQuizzes),
                    "https://img.icons8.com/fluency/48/task.png");
            document.add(summaryTable);

            // Data Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 4.5f, 2.5f, 1.5f });
            table.setHeaderRows(1);

            String[] headers = { "Type", "Description", "Date", "Status" };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FONT_TABLE_HEADER));
                cell.setBackgroundColor(BRAND_COLOR);
                cell.setPadding(10);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBorderWidth(0);
                table.addCell(cell);
            }

            DateTimeFormatter rowFmt = DateTimeFormatter.ofPattern("MMM dd, hh:mm a");
            boolean alternate = false;

            for (SystemEventDTO log : logs) {
                BaseColor rowColor = alternate ? HEADER_BG : BaseColor.WHITE;
                addRowCell(table, log.getType(), rowColor);
                addRowCell(table, log.getDescription(), rowColor);
                addRowCell(table, log.getDate() != null ? log.getDate().format(rowFmt) : "-", rowColor);

                Font statusFont = FONT_BODY;
                if ("Success".equals(log.getStatus()))
                    statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new BaseColor(34, 197, 94));
                else if ("Failed".equals(log.getStatus()))
                    statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BaseColor.RED);

                PdfPCell statusCell = new PdfPCell(new Phrase(log.getStatus(), statusFont));
                statusCell.setBackgroundColor(rowColor);
                statusCell.setPadding(8);
                statusCell.setBorderColor(new BaseColor(226, 232, 240));
                table.addCell(statusCell);
                alternate = !alternate;
            }

            document.add(table);
            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- 3. CSV EXPORT ---
    public byte[] generateCsvReport(LocalDate startDate, LocalDate endDate) {
        List<SystemEventDTO> logs = getSystemEvents(startDate, endDate);
        StringBuilder sb = new StringBuilder();
        sb.append("Type,Description,Date,Status\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
        for (SystemEventDTO log : logs) {
            sb.append(log.getType()).append(",")
                    .append("\"").append(log.getDescription().replace("\"", "\"\"")).append("\",")
                    .append(log.getDate() != null ? log.getDate().format(fmt) : "").append(",")
                    .append(log.getStatus()).append("\n");
        }
        return sb.toString().getBytes();
    }

    // --- 4. DYNAMIC CHART DATA ---
    public List<Double> getRevenueChartData() {
        List<Double> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 3; i >= 0; i--) {
            LocalDateTime start = now.minusWeeks(i + 1);
            LocalDateTime end = now.minusWeeks(i);

            double weeklyRevenue = paymentRepository.findAll().stream()
                    .filter(p -> p.getCreatedAt() != null)
                    .filter(p -> !p.getCreatedAt().isBefore(start) && !p.getCreatedAt().isAfter(end))
                    .filter(p -> isSuccessful(p.getStatus()))
                    .mapToDouble(p -> parseAmountFromString(p.getAmount()))
                    .sum();
            data.add(weeklyRevenue);
        }
        return data;
    }

    public List<Integer> getUserChartData() {
        List<Integer> data = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Loop: End of Week 1 -> End of Week 2 -> ... -> Today
        for (int i = 3; i >= 0; i--) {
            LocalDateTime endTime = now.minusWeeks(i);

            long totalUsersUntilThen = userRepository.findAll().stream()
                    .filter(u -> u.getCreatedAt() != null)
                    .filter(u -> !u.getCreatedAt().isAfter(endTime)) // Count everyone created BEFORE this point
                    .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                    .count();

            data.add((int) totalUsersUntilThen);
        }
        return data;
    }

    // --- HELPERS ---
    private boolean isSuccessful(String status) {
        if (status == null)
            return false;
        String s = status.trim().toLowerCase();
        return s.equals("captured") || s.equals("paid") || s.equals("success");
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(amount);
    }

    private String formatCurrency(String amountStr) {
        try {
            return formatCurrency(Double.parseDouble(amountStr));
        } catch (Exception e) {
            return "INR " + amountStr;
        }
    }

    private double parseAmountFromString(String amountStr) {
        try {
            return Double.parseDouble(amountStr);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double parseAmountFromDesc(String desc) {
        try {
            // Split by space and find the one containing a decimal or number
            // Expected format: "Payment: 899.00 (..."
            String[] parts = desc.split(" ");
            for (String part : parts) {
                // Remove currency symbols/formatting
                String clean = part.replaceAll("[^0-9.]", "");
                if (!clean.isEmpty() && clean.contains(".")) {
                    return Double.parseDouble(clean);
                }
            }
            return 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void addSummaryCard(PdfPTable table, String title, String value, String iconUrl) {
        PdfPCell wrapper = new PdfPCell();
        wrapper.setBorder(Rectangle.NO_BORDER);
        wrapper.setPadding(5);

        PdfPTable card = new PdfPTable(2);
        card.setWidthPercentage(100);
        try {
            card.setWidths(new float[] { 1f, 3f });
        } catch (Exception e) {
        }

        PdfPCell iconCell = new PdfPCell();
        iconCell.setBackgroundColor(HEADER_BG);
        iconCell.setBorderColor(new BaseColor(226, 232, 240));
        iconCell.setBorderWidthTop(1f);
        iconCell.setBorderWidthBottom(1f);
        iconCell.setBorderWidthLeft(1f);
        iconCell.setBorderWidthRight(0);
        iconCell.setPadding(10);
        iconCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        iconCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            Image icon = Image.getInstance(new URL(iconUrl));
            icon.scaleToFit(24, 24);
            iconCell.addElement(icon);
        } catch (Exception e) {
        }
        card.addCell(iconCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBackgroundColor(HEADER_BG);
        textCell.setBorderColor(new BaseColor(226, 232, 240));
        textCell.setBorderWidthTop(1f);
        textCell.setBorderWidthBottom(1f);
        textCell.setBorderWidthRight(1f);
        textCell.setBorderWidthLeft(0);
        textCell.setPadding(10);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        textCell.addElement(new Paragraph(title, FONT_SUBTITLE));
        Paragraph pValue = new Paragraph(value, FONT_SUMMARY_VAL);
        pValue.setSpacingBefore(2f);
        textCell.addElement(pValue);
        card.addCell(textCell);

        wrapper.addElement(card);
        table.addCell(wrapper);
    }

    private void addRowCell(PdfPTable table, String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        table.addCell(cell);
    }

    class HeaderFooterPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorStroke(new BaseColor(226, 232, 240));
            cb.setLineWidth(1);
            cb.moveTo(document.left(), document.bottom() - 10);
            cb.lineTo(document.right(), document.bottom() - 10);
            cb.stroke();
            try {
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("Confidential - Generated by GearNest", FONT_SUBTITLE),
                        (document.right() - document.left()) / 2 + document.leftMargin(), document.bottom() - 25, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                        new Phrase("Page " + writer.getPageNumber(), FONT_SUBTITLE),
                        document.right(), document.bottom() - 25, 0);
            } catch (Exception e) {
            }
        }
    }
}