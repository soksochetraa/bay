package com.example.bay.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    public static byte[] compressImage(Context context, Uri imageUri, int maxSizeKB) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (bitmap == null) {
                return null;
            }

            bitmap = fixRotation(context, imageUri, bitmap);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = 85;
            int maxSizeBytes = maxSizeKB * 1024;

            do {
                outputStream.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                quality -= 5;
            } while (outputStream.size() > maxSizeBytes && quality > 20);

            bitmap.recycle();

            byte[] result = outputStream.toByteArray();
            outputStream.close();

            Log.d(TAG, "Compressed image to " + result.length + " bytes, quality: " + quality);
            return result;

        } catch (IOException e) {
            Log.e(TAG, "Error compressing image: " + e.getMessage());
            return null;
        }
    }

    public static byte[] createThumbnail(Context context, Uri imageUri, int maxDimension) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return null;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null;
            }

            options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension);
            options.inJustDecodeBounds = false;

            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap == null) {
                return null;
            }

            bitmap = fixRotation(context, imageUri, bitmap);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);

            bitmap.recycle();

            byte[] result = outputStream.toByteArray();
            outputStream.close();

            Log.d(TAG, "Created thumbnail: " + result.length + " bytes");
            return result;

        } catch (IOException e) {
            Log.e(TAG, "Error creating thumbnail: " + e.getMessage());
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap fixRotation(Context context, Uri imageUri, Bitmap bitmap) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return bitmap;
            }

            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            inputStream.close();

            Matrix matrix = new Matrix();

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.postScale(1, -1);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.postRotate(90);
                    matrix.postScale(-1, 1);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.postRotate(270);
                    matrix.postScale(-1, 1);
                    break;
                default:
                    return bitmap;
            }

            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();

            return rotatedBitmap;

        } catch (IOException e) {
            Log.e(TAG, "Error fixing rotation: " + e.getMessage());
            return bitmap;
        }
    }

    public static int[] getImageDimensions(Context context, Uri imageUri) {
        int[] dimensions = new int[2];

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                dimensions[0] = 0;
                dimensions[1] = 0;
                return dimensions;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            inputStream.close();

            dimensions[0] = options.outWidth;
            dimensions[1] = options.outHeight;

            Log.d(TAG, "Image dimensions: " + dimensions[0] + "x" + dimensions[1]);

        } catch (IOException e) {
            Log.e(TAG, "Error getting image dimensions: " + e.getMessage());
            dimensions[0] = 0;
            dimensions[1] = 0;
        }

        return dimensions;
    }

    public static File saveBitmapToFile(Context context, Bitmap bitmap, String fileName) {
        try {
            File file = new File(context.getCacheDir(), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            outputStream.flush();
            outputStream.close();
            return file;
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file: " + e.getMessage());
            return null;
        }
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            return fixRotation(context, uri, bitmap);

        } catch (IOException e) {
            Log.e(TAG, "Error getting bitmap from URI: " + e.getMessage());
            return null;
        }
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream);
        byte[] byteArray = stream.toByteArray();
        try {
            stream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing stream: " + e.getMessage());
        }
        return byteArray;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        if (bitmap == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap;
        }

        float ratio = (float) width / (float) height;

        if (ratio > 1) {
            width = maxWidth;
            height = (int) (width / ratio);
        } else {
            height = maxHeight;
            width = (int) (height * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public static Uri getUriFromBitmap(Context context, Bitmap bitmap) {
        File file = saveBitmapToFile(context, bitmap, "temp_image_" + System.currentTimeMillis() + ".jpg");
        if (file != null) {
            return Uri.fromFile(file);
        }
        return null;
    }

    public static String getImageExtension(Context context, Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType != null) {
                if (mimeType.equals("image/jpeg") || mimeType.equals("image/jpg")) {
                    return "jpg";
                } else if (mimeType.equals("image/png")) {
                    return "png";
                } else if (mimeType.equals("image/gif")) {
                    return "gif";
                } else if (mimeType.equals("image/webp")) {
                    return "webp";
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting image extension: " + e.getMessage());
        }
        return "jpg"; // default extension
    }

    public static long getImageSize(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return 0;
            }

            long size = 0;
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                size += bytesRead;
            }

            inputStream.close();
            return size;

        } catch (IOException e) {
            Log.e(TAG, "Error getting image size: " + e.getMessage());
            return 0;
        }
    }
}