package com.example.akherapp.utils;

import android.content.Context;
import android.os.Environment;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.pdf.BaseFont;
import com.example.akherapp.User;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class PdfExporter {
    public static File exportUsersToPdf(Context context, List<User> users) throws Exception {
        String fileName = "users_list_" + System.currentTimeMillis() + ".pdf";
        File pdfFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        // Create Arabic Font
        BaseFont arabicBaseFont = BaseFont.createFont(
                "assets/fonts/noto_naskh_arabic.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED,
                true,
                readFontBytes(context, "fonts/noto_naskh_arabic.ttf"),
                null
        );

        Font arabicFont = new Font(arabicBaseFont, 12, Font.NORMAL, BaseColor.BLACK);
        Font arabicTitleFont = new Font(arabicBaseFont, 20, Font.BOLD, BaseColor.BLACK);
        Font arabicHeaderFont = new Font(arabicBaseFont, 12, Font.BOLD, BaseColor.WHITE);

        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
        document.open();

        // Add mosque logo
        // Modify the logo loading part
        try {
            byte[] logoBytes = readAndResizeImage(context, "images/mosque_logo.png", 300, 300);
            if (logoBytes != null) {
                com.itextpdf.text.Image mosqueLogo = com.itextpdf.text.Image.getInstance(logoBytes);
                mosqueLogo.scaleToFit(100, 100);
                mosqueLogo.setAlignment(Element.ALIGN_CENTER);
                document.add(mosqueLogo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Continue without logo if image loading fails
        }

        // Add mosque name
        PdfPTable mosqueNameTable = new PdfPTable(1);
        mosqueNameTable.setWidthPercentage(100);
        mosqueNameTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        PdfPCell mosqueNameCell = new PdfPCell(new Paragraph("كتاب مسجد الإمام مالك", arabicTitleFont));
        mosqueNameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        mosqueNameCell.setBorder(PdfPCell.NO_BORDER);
        mosqueNameCell.setPadding(10);

        mosqueNameTable.addCell(mosqueNameCell);
        document.add(mosqueNameTable);
        document.add(new Paragraph(" "));

        // Add title with Arabic font
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        PdfPCell titleCell = new PdfPCell(new Paragraph("قائمة الطلبة", arabicTitleFont));
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setBorder(PdfPCell.NO_BORDER);
        titleCell.setPadding(10);

        titleTable.addCell(titleCell);
        document.add(titleTable);
        document.add(new Paragraph(" "));

        // Create table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        // Table headers with Arabic font
        BaseColor headerColor = new BaseColor(103, 58, 183);
        String[] headers = {"الاسم الكامل", "المعلم", "رقم الهاتف", "العمر", "تاريخ التسجيل"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Paragraph(header, arabicHeaderFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(headerColor);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Add data with Arabic font
        for (User user : users) {
            addCell(table, user.getFirstName() + " " + user.getLastName(), arabicFont);
            addCell(table, user.getTeacher() != null ? user.getTeacher() : "", arabicFont);
            addCell(table, user.getPhone() != null ? user.getPhone() : "", arabicFont);

            String age = "";
            if (user.getBirthDate() != null) {
                int userAge = DateUtils.calculateAge(user.getBirthDate());
                age = userAge > 0 ? String.valueOf(userAge) : "";
            }
            addCell(table, age, arabicFont);

            addCell(table, user.getRegistrationDate() != null ?
                    DateUtils.formatDate(user.getRegistrationDate()) : "", arabicFont);
        }

        document.add(table);
        document.close();

        return pdfFile;
    }

    private static void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    // Add this new method at the end of the class
    private static byte[] readFontBytes(Context context, String fontPath) throws IOException {
        InputStream is = context.getAssets().open(fontPath);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        is.close();
        return buffer.toByteArray();
    }

    // Add this new method to resize the image
    private static byte[] readAndResizeImage(Context context, String imagePath, int maxWidth, int maxHeight) {
        try {
            InputStream is = context.getAssets().open(imagePath);
            
            // Decode bounds first
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            // Calculate sample size
            int sampleSize = 1;
            if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                sampleSize = Math.max(
                    options.outWidth / maxWidth,
                    options.outHeight / maxHeight
                );
            }

            // Decode with sample size
            is = context.getAssets().open(imagePath);
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (bitmap == null) {
                return null;
            }

            // Scale if needed
            float scale = Math.min(
                (float) maxWidth / bitmap.getWidth(),
                (float) maxHeight / bitmap.getHeight()
            );
            
            if (scale < 1) {
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            // Convert to PNG bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 80, baos);
            byte[] imageData = baos.toByteArray();
            
            // Clean up
            bitmap.recycle();
            baos.close();
            
            return imageData;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}