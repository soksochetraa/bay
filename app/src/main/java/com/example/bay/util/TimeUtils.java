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

    public static String formatTimeAgo(String timestamp) {
        try {
            long time;
            if (timestamp.matches("\\d+")) {
                time = Long.parseLong(timestamp);
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                Date date = sdf.parse(timestamp);
                time = date.getTime();
            }

            long now = System.currentTimeMillis();
            long diff = now - time;

            if (diff < 1000) {
                return "អម្បាញ់មិញ";
            } else if (diff < 60000) {
                long seconds = diff / 1000;
                return seconds + " វិនាទីមុន";
            } else if (diff < 3600000) {
                long minutes = diff / 60000;
                return minutes + " នាទីមុន";
            } else if (diff < 86400000) {
                long hours = diff / 3600000;
                return hours + " ម៉ោងមុន";
            } else if (diff < 604800000) {
                long days = diff / 86400000;
                return days + " ថ្ងៃមុន";
            } else if (diff < 2592000000L) {
                long weeks = diff / 604800000;
                return weeks + " សប្តាហ៍មុន";
            } else if (diff < 31536000000L) {
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

    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String getChatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 24 * 60 * 60 * 1000) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } else if (diff < 7 * 24 * 60 * 60 * 1000) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static String getMessageTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "អម្បាញ់មិញ";
        } else if (diff < 3600000) {
            long minutes = diff / 60000;
            return minutes + " នាទី";
        } else if (diff < 86400000) {
            long hours = diff / 3600000;
            return hours + " ម៉ោង";
        } else if (diff < 604800000) {
            long days = diff / 86400000;
            return days + " ថ្ងៃ";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static String getDayOfWeek(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String getShortTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static boolean isToday(long timestamp) {
        Date date = new Date(timestamp);
        Date today = new Date();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(date).equals(sdf.format(today));
    }

    public static boolean isYesterday(long timestamp) {
        Date date = new Date(timestamp);
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(date).equals(sdf.format(yesterday));
    }

    public static boolean isSameDay(long timestamp1, long timestamp2) {
        Date date1 = new Date(timestamp1);
        Date date2 = new Date(timestamp2);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return sdf.format(date1).equals(sdf.format(date2));
    }
}