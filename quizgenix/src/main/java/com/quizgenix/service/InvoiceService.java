package com.quizgenix.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.quizgenix.model.Payment;
import com.quizgenix.repository.PaymentRepository;

@Service
public class InvoiceService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EmailService emailService; // Using the new EmailService

    // --- COLORS ---
    private static final BaseColor BRAND_COLOR = new BaseColor(139, 92, 246);
    private static final BaseColor TABLE_HEADER_BG = new BaseColor(241, 245, 249);
    private static final BaseColor TEXT_COLOR = new BaseColor(30, 41, 59);

    // --- FONTS ---
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, BRAND_COLOR);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.GRAY);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
    private static final Font FONT_BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT_COLOR);
    private static final Font FONT_BODY_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_COLOR);

    // --- 1. GENERATE PDF ---
    public byte[] generateInvoicePdf(Payment payment) throws DocumentException {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // 1. HEADER (Side-by-Side Logo)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new int[] { 6, 4 });

        // Left Side: Brand
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        PdfPTable logoTable = new PdfPTable(2);
        logoTable.setWidthPercentage(100);
        try {
            logoTable.setWidths(new float[] { 1f, 4f });
        } catch (Exception e) {
        }

        PdfPCell imgCell = new PdfPCell();
        imgCell.setBorder(Rectangle.NO_BORDER);
        imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            Image logo = Image.getInstance(new ClassPathResource("static/images/logo.png").getURL());
            logo.scaleToFit(50, 50);
            imgCell.addElement(logo);
        } catch (Exception e) {
            imgCell.addElement(new Phrase(" "));
        }
        logoTable.addCell(imgCell);

        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph brandName = new Paragraph("QuizGenix", FONT_TITLE);
        brandName.setSpacingBefore(8f);
        textCell.addElement(brandName);
        logoTable.addCell(textCell);

        brandCell.addElement(logoTable);
        headerTable.addCell(brandCell);

        // Right Side: Meta
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(Rectangle.NO_BORDER);
        metaCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invoiceLabel = new Paragraph("INVOICE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.LIGHT_GRAY));
        invoiceLabel.setAlignment(Element.ALIGN_RIGHT);
        metaCell.addElement(invoiceLabel);
        metaCell.addElement(createRightAlignedLine("Invoice #: ", payment.getPaymentId()));
        metaCell.addElement(createRightAlignedLine("Date: ",
                payment.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
        metaCell.addElement(createRightAlignedLine("Status: ", payment.getStatus().toUpperCase()));
        headerTable.addCell(metaCell);

        document.add(headerTable);

        // Divider
        LineSeparator ls = new LineSeparator();
        ls.setLineColor(new BaseColor(226, 232, 240));
        ls.setOffset(-10);
        document.add(new Paragraph("\n"));
        document.add(ls);
        document.add(new Paragraph("\n"));

        // 2. BILLING
        PdfPTable billTable = new PdfPTable(2);
        billTable.setWidthPercentage(100);

        PdfPCell fromCell = new PdfPCell();
        fromCell.setBorder(Rectangle.NO_BORDER);
        fromCell.addElement(new Paragraph("Payable To:", FONT_SUBTITLE));
        fromCell.addElement(new Paragraph("QuizGenix Inc.", FONT_BODY_BOLD));
        fromCell.addElement(new Paragraph("123 Tech Park, Suite 400", FONT_BODY));
        fromCell.addElement(new Paragraph("Surat, Gujarat, India - 395006", FONT_BODY));
        billTable.addCell(fromCell);

        PdfPCell toCell = new PdfPCell();
        toCell.setBorder(Rectangle.NO_BORDER);
        String userName = payment.getUser() != null
                ? capitalizeWords(payment.getUser().getFirstName() + " " + payment.getUser().getLastName())
                : "Valued Customer";
        String userEmail = payment.getUser() != null ? payment.getUser().getEmail() : payment.getArchivedUserEmail();
        toCell.addElement(new Paragraph("Bill To:", FONT_SUBTITLE));
        toCell.addElement(new Paragraph(userName, FONT_BODY_BOLD));
        toCell.addElement(new Paragraph(userEmail, FONT_BODY));
        billTable.addCell(toCell);

        document.add(billTable);
        document.add(new Paragraph("\n\n"));

        // 3. ITEMS
        PdfPTable itemTable = new PdfPTable(4);
        itemTable.setWidthPercentage(100);
        itemTable.setWidths(new float[] { 4f, 1f, 1.5f, 1.5f });
        itemTable.setHeaderRows(1);

        addTableHeader(itemTable, "Description");
        addTableHeader(itemTable, "Qty");
        addTableHeader(itemTable, "Price");
        addTableHeader(itemTable, "Total");

        String planDisplay = payment.getPlanName();
        if ("Monthly Plan".equals(planDisplay))
            planDisplay = "Pro Plan (Monthly)";
        else if ("6-Month Plan".equals(planDisplay))
            planDisplay = "Plus Plan (6 Months)";
        else if ("Yearly Plan".equals(planDisplay))
            planDisplay = "Premium Plan (Yearly)";

        addTableRow(itemTable, "QuizGenix Subscription - " + planDisplay, Element.ALIGN_LEFT);
        addTableRow(itemTable, "1", Element.ALIGN_CENTER);
        addTableRow(itemTable, "INR " + payment.getAmount(), Element.ALIGN_RIGHT);
        addTableRow(itemTable, "INR " + payment.getAmount(), Element.ALIGN_RIGHT);

        addEmptyRow(itemTable);
        document.add(itemTable);

        // 4. TOTALS
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(40);
        totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTotalRow(totalTable, "Subtotal:", "INR " + payment.getAmount(), false);
        addTotalRow(totalTable, "Total:", "INR " + payment.getAmount(), true);
        document.add(totalTable);

        // 5. FOOTER
        document.add(new Paragraph("\n\n\n"));
        Paragraph footer = new Paragraph("Thank you for choosing QuizGenix!", FONT_SUBTITLE);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph notice = new Paragraph(
                "If you have any questions about this invoice, please contact support@QuizGenix.com",
                FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY));
        notice.setAlignment(Element.ALIGN_CENTER);
        document.add(notice);

        document.close();
        return out.toByteArray();
    }

    // --- 2. SEND INVOICE (Delegate to EmailService) ---
    public boolean sendInvoiceEmail(Long paymentId) {
        System.out.println("--- STARTING EMAIL SEND PROCESS ---");
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                System.err.println("Payment not found");
                return false;
            }

            System.out.println("Step 1: Generating PDF...");
            byte[] pdfBytes = generateInvoicePdf(payment);

            System.out.println("Step 2: Delegating to EmailService...");
            emailService.sendInvoiceEmail(payment.getUser(), payment.getArchivedUserEmail(), payment.getPaymentId(),
                    pdfBytes);

            System.out.println("--- EMAIL SENT SUCCESSFULLY ---");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // --- HELPERS ---
    private Paragraph createRightAlignedLine(String label, String value) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_RIGHT);
        p.add(new Chunk(label, FONT_BODY));
        p.add(new Chunk(value, FONT_BODY_BOLD));
        return p;
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(TABLE_HEADER_BG);
        header.setBorderWidth(0);
        header.setBorderWidthBottom(1f);
        header.setBorderColorBottom(BaseColor.LIGHT_GRAY);
        header.setPadding(10);
        header.setPhrase(new Phrase(headerTitle, FONT_HEADER));
        table.addCell(header);
    }

    private void addTableRow(PdfPTable table, String text, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setPadding(10);
        cell.setBorderWidth(0);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderColorBottom(new BaseColor(241, 245, 249));
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addEmptyRow(PdfPTable table) {
        for (int i = 0; i < 4; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(" "));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean isFinal) {
        Font f = isFinal ? FONT_BODY_BOLD : FONT_BODY;
        PdfPCell l = new PdfPCell(new Phrase(label, f));
        l.setBorder(Rectangle.NO_BORDER);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(5);
        PdfPCell v = new PdfPCell(new Phrase(value, f));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(5);
        if (isFinal) {
            v.setBorderWidthTop(1f);
            v.setBorderColorTop(BaseColor.LIGHT_GRAY);
        }
        table.addCell(l);
        table.addCell(v);
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isEmpty())
            return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words)
            if (w.length() > 0)
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
        return sb.toString().trim();
    }
}