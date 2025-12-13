package com.example.bay.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static String getTimeAgo(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "មិនទាន់មាន";
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date past = sdf.parse(timestamp);

            if (past == null) {
                return "មិនទាន់មាន";
            }

            long now = System.currentTimeMillis();
            long timeDiff = now - past.getTime();

            long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff);
            long hours = TimeUnit.MILLISECONDS.toHours(timeDiff);
            long days = TimeUnit.MILLISECONDS.toDays(timeDiff);
            long weeks = days / 7;
            long months = days / 30;
            long years = days / 365;

            if (seconds < 60) {
                return "អម្បាញ់មិញ";
            } else if (minutes < 60) {
                return minutes + " នាទីមុន";
            } else if (hours < 24) {
                return hours + " ម៉ោងមុន";
            } else if (days < 7) {
                return days + " ថ្ងៃមុន";
            } else if (weeks < 4) {
                return weeks + " សប្តាហ៍មុន";
            } else if (months < 12) {
                return months + " ខែមុន";
            } else {
                return years + " ឆ្នាំមុន";
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return "មិនទាន់មាន";
        }
    }

    public static String getRelativeTime(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "មិនទាន់មាន";
        }

        try {
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
                    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
            };

            Date past = null;
            for (SimpleDateFormat format : formats) {
                try {
                    past = format.parse(timestamp);
                    if (past != null) break;
                } catch (ParseException e) {

                }
            }

            if (past == null) {
                return "មិនទាន់មាន";
            }

            long now = System.currentTimeMillis();
            long timeDiff = now - past.getTime();

            if (timeDiff < 0) {
                return "អម្បាញ់មិញ";
            }

            long seconds = timeDiff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            if (seconds < 60) {
                return "អម្បាញ់មិញ";
            } else if (minutes == 1) {
                return "1 នាទីមុន";
            } else if (minutes < 60) {
                return minutes + " នាទីមុន";
            } else if (hours == 1) {
                return "1 ម៉ោងមុន";
            } else if (hours < 24) {
                return hours + " ម៉ោងមុន";
            } else if (days == 1) {
                return "ម្សិលមិញ";
            } else if (days < 7) {
                return days + " ថ្ងៃមុន";
            } else if (days < 30) {
                long weeks = days / 7;
                if (weeks == 1) {
                    return "1 សប្តាហ៍មុន";
                }
                return weeks + " សប្តាហ៍មុន";
            } else if (days < 365) {
                long months = days / 30;
                if (months == 1) {
                    return "1 ខែមុន";
                }
                return months + " ខែមុន";
            } else {
                long years = days / 365;
                if (years == 1) {
                    return "1 ឆ្នាំមុន";
                }
                return years + " ឆ្នាំមុន";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "មិនទាន់មាន";
        }
    }

    // Alternative simpler version
    public static String formatTimeAgo(String timestamp) {
        try {
            // Assume Firebase timestamp format (can be String or Long)
            long time;
            if (timestamp.matches("\\d+")) {
                // It's a numeric timestamp (milliseconds)
                time = Long.parseLong(timestamp);
            } else {
                // Try to parse as date string
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                Date date = sdf.parse(timestamp);
                time = date.getTime();
            }

            long now = System.currentTimeMillis();
            long diff = now - time;

            if (diff < 1000) { // Less than 1 second
                return "អម្បាញ់មិញ";
            } else if (diff < 60000) { // Less than 1 minute
                long seconds = diff / 1000;
                return seconds + " វិនាទីមុន";
            } else if (diff < 3600000) { // Less than 1 hour
                long minutes = diff / 60000;
                return minutes + " នាទីមុន";
            } else if (diff < 86400000) { // Less than 1 day
                long hours = diff / 3600000;
                return hours + " ម៉ោងមុន";
            } else if (diff < 604800000) { // Less than 1 week
                long days = diff / 86400000;
                return days + " ថ្ងៃមុន";
            } else if (diff < 2592000000L) { // Less than 1 month (30 days)
                long weeks = diff / 604800000;
                return weeks + " សប្តាហ៍មុន";
            } else if (diff < 31536000000L) { // Less than 1 year
                long months = diff / 2592000000L;
                return months + " ខែមុន";
            } else {
                long years = diff / 31536000000L;
                return years + " ឆ្នាំមុន";
            }

        } catch (Exception e) {
            return "មិនទាន់មាន";
        }
    }
}