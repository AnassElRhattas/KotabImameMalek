package com.example.akherapp.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    // Modifier le format de date pour utiliser Locale.FRENCH au lieu de Locale("ar")
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

    public static int calculateAge(String birthDateMillis) {
        try {
            long birthDateLong = Long.parseLong(birthDateMillis);
            Calendar birthDate = Calendar.getInstance();
            birthDate.setTimeInMillis(birthDateLong);

            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);

            // Check if birthday hasn't occurred this year
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }

            return age;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String formatDate(String birthDateMillis) {
        try {
            long birthDateLong = Long.parseLong(birthDateMillis);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(new Date(birthDateLong));
        } catch (NumberFormatException e) {
            return "غير متوفر";
        }
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return dateFormat.format(date);
    }
}
