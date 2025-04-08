package com.example.akherapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import android.net.Uri;

public class CertificateGenerator {

    public static File generateCertificate(Context context, User user) throws IOException {
        try {
            // Output path
            File outputDir = new File(context.getFilesDir(), "certificates");
            if (!outputDir.exists()) outputDir.mkdirs();

            String fileName = "certificate_" + user.getFirstName() + " " + user.getLastName() + ".pdf";
            File outputFile = new File(outputDir, fileName);

            // Create document
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            // Load Arabic font
            BaseFont baseFont = BaseFont.createFont(
                    "assets/fonts/Al-samt-tow.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    readFontBytes(context, "fonts/Al-samt-tow.ttf"),
                    null
            );
            Font arabicFont = new Font(baseFont, 40, Font.BOLD, BaseColor.BLACK);

            // Add background image
            byte[] bgBytes = readAndResizeImage(context, "images/certificate_background.jpg", 1920, 1080);
            if (bgBytes != null) {
                Image background = Image.getInstance(bgBytes);
                background.setAbsolutePosition(0, 0);
                background.scaleAbsolute(PageSize.A4.rotate());
                document.add(background);
            }

            // Add user photo if available
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Task<DocumentSnapshot> task = db.collection("users").document(user.getId()).get();
            DocumentSnapshot documentSnapshot = Tasks.await(task);
            
            Map<String, Object> documents = (Map<String, Object>) documentSnapshot.get("documents");
            if (documents != null && documents.containsKey("photo")) {
                String photoUrl = (String) documents.get("photo");
                try {
                    byte[] photoBytes = downloadImageFromUrl(photoUrl);
                    if (photoBytes != null) {
                        Image userPhoto = Image.getInstance(photoBytes);
                        userPhoto.scaleToFit(100, 100);
                        userPhoto.setAbsolutePosition(
                            PageSize.A4.rotate().getWidth() - 220,
                            PageSize.A4.rotate().getHeight() - 290
                        );
                        document.add(userPhoto);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Continue with name addition
            String fullName = user.getFirstName() + " " + user.getLastName();

            PdfPTable nameTable = new PdfPTable(1);
            nameTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            nameTable.setWidthPercentage(60); // Adjusted width
            nameTable.setHorizontalAlignment(Element.ALIGN_CENTER); // Center alignment

            PdfPCell nameCell = new PdfPCell(new Paragraph(fullName, arabicFont));
            nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            nameCell.setBorder(PdfPCell.NO_BORDER);
            nameCell.setPaddingTop(230); // Adjusted to match the dots position
            nameCell.setPaddingBottom(20);
            nameCell.setPaddingRight(250);

            nameTable.addCell(nameCell);
            document.add(nameTable);

            document.close();

            // Upload certificate to Firebase Storage
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("certificates")
                .child(user.getId())
                .child(fileName);

            Uri fileUri = Uri.fromFile(outputFile);
            UploadTask uploadTask = storageRef.putFile(fileUri);

            // Get download URL and update Firestore
            Task<Uri> urlTask = uploadTask.continueWithTask(uploadResult -> {
                if (!uploadResult.isSuccessful()) {
                    throw uploadResult.getException();
                }
                return storageRef.getDownloadUrl();
            }).addOnSuccessListener(uri -> {
                // Update Firestore with certificate URL
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getId())
                    .update("certificateUrl", uri.toString());
            });

            Tasks.await(urlTask); // Wait for upload to complete
            return outputFile;

        } catch (Exception e) {
            throw new IOException("Erreur lors de la génération du certificat : " + e.getMessage(), e);
        }
    }

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

    private static byte[] readAndResizeImage(Context context, String imagePath, int maxWidth, int maxHeight) {
        try {
            InputStream is = context.getAssets().open(imagePath);

            // Get image bounds
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            int sampleSize = 1;
            if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                sampleSize = Math.max(options.outWidth / maxWidth, options.outHeight / maxHeight);
            }

            is = context.getAssets().open(imagePath);
            options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize;
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (bitmap == null) return null;

            float scale = Math.min((float) maxWidth / bitmap.getWidth(), (float) maxHeight / bitmap.getHeight());
            if (scale < 1) {
                int newWidth = Math.round(bitmap.getWidth() * scale);
                int newHeight = Math.round(bitmap.getHeight() * scale);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            bitmap.recycle();
            baos.close();

            return baos.toByteArray();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static byte[] downloadImageFromUrl(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = input.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static File generateAllCertificates(Context context, List<User> users) throws IOException {
        try {
            // Create output directory
            File outputDir = new File(context.getFilesDir(), "certificates");
            if (!outputDir.exists()) outputDir.mkdirs();

            String fileName = "all_certificates_" + System.currentTimeMillis() + ".pdf";
            File outputFile = new File(outputDir, fileName);

            // Create document
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            // Load Arabic font
            BaseFont baseFont = BaseFont.createFont(
                    "assets/fonts/Al-samt-tow.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    readFontBytes(context, "fonts/Al-samt-tow.ttf"),
                    null
            );
            Font arabicFont = new Font(baseFont, 40, Font.BOLD, BaseColor.BLACK);

            // Generate certificate for each user
            for (User user : users) {
                document.newPage();
                
                // Add background image
                byte[] bgBytes = readAndResizeImage(context, "images/certificate_background.jpg", 1920, 1080);
                if (bgBytes != null) {
                    Image background = Image.getInstance(bgBytes);
                    background.setAbsolutePosition(0, 0);
                    background.scaleAbsolute(PageSize.A4.rotate());
                    document.add(background);
                }

                // Add user photo if available
                try {
                    Map<String, Object> documents = user.getDocuments();
                    if (documents != null && documents.containsKey("photo")) {
                        String photoUrl = (String) documents.get("photo");
                        byte[] photoBytes = downloadImageFromUrl(photoUrl);
                        if (photoBytes != null) {
                            Image userPhoto = Image.getInstance(photoBytes);
                            userPhoto.scaleToFit(100, 100);
                            userPhoto.setAbsolutePosition(
                                PageSize.A4.rotate().getWidth() - 220,
                                PageSize.A4.rotate().getHeight() - 290
                            );
                            document.add(userPhoto);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Add user name
                String fullName = user.getFirstName() + " " + user.getLastName();
                PdfPTable nameTable = new PdfPTable(1);
                nameTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
                nameTable.setWidthPercentage(60);
                nameTable.setHorizontalAlignment(Element.ALIGN_CENTER);

                PdfPCell nameCell = new PdfPCell(new Paragraph(fullName, arabicFont));
                nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                nameCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                nameCell.setBorder(PdfPCell.NO_BORDER);
                nameCell.setPaddingTop(230);
                nameCell.setPaddingBottom(20);
                nameCell.setPaddingRight(250);

                nameTable.addCell(nameCell);
                document.add(nameTable);
            }

            document.close();

            // Upload to Firebase Storage
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("certificates")
                .child("all")
                .child(fileName);

            Uri fileUri = Uri.fromFile(outputFile);
            UploadTask uploadTask = storageRef.putFile(fileUri);

            Tasks.await(uploadTask);
            return outputFile;

        } catch (Exception e) {
            throw new IOException("Erreur lors de la génération des certificats : " + e.getMessage(), e);
        }
    }
}
