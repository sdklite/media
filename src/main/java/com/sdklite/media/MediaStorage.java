package com.sdklite.media;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Media storage manager
 */
public class MediaStorage {

    private static final String TAG = "MediaStorage";

    public static final long DISK_UNAVAILABLE = -1L;
    public static final long DISK_PREPARING = -2L;
    public static final long DISK_UNKNOWN_SIZE = -3L;

    private static final boolean HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT = hasField(MediaStore.MediaColumns.class, "WIDTH") && hasField(MediaStore.MediaColumns.class, "HEIGHT");

    private static final String DCIM_CAMERA = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + File.separator + "Camera";

    private static boolean hasField(final Class<?> clazz, final String field) {
        try {
            clazz.getDeclaredField(field);
            return true;
        } catch (final NoSuchFieldException e) {
            return false;
        }
    }

    private MediaStorage() {
    }

    /**
     * Returns the available space of external storage in bytes
     *
     * @return the available space of external storage in bytes or negative value if external storage unavailable
     * @see #DISK_PREPARING
     * @see #DISK_UNAVAILABLE
     * @see #DISK_UNKNOWN_SIZE
     */
    public static long getAvailableSpace() {
        final String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_CHECKING.equalsIgnoreCase(state)) {
            return DISK_PREPARING;
        }

        if (!Environment.MEDIA_MOUNTED.equalsIgnoreCase(state)) {
            return DISK_UNAVAILABLE;
        }

        final File dir = Environment.getExternalStorageDirectory();
        dir.mkdirs();
        if ((!dir.isDirectory()) || (!dir.canWrite())) {
            return DISK_UNAVAILABLE;
        }

        try {
            final StatFs stat = new StatFs(dir.getPath());
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (final Exception e) {
            return DISK_UNKNOWN_SIZE;
        }
    }

    /**
     * Generate a filename without extension
     *
     * @return a filename without extension
     */
    public static String generateFilename() {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }

    public static String generateFilename(final String ext) {
        return generateFilename() + ext;
    }

    /**
     * Generate an image path under {@code Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)/Camera}
     *
     * @return an image path
     */
    public static String generatePicturePath() {
        return generatePicturePath(generateFilename(".jpeg"));
    }

    /**
     * Generate an image path under {@code Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)/Camera} with the specified filename
     *
     * @param filename
     *            The filename
     * @return an image path
     */
    public static String generatePicturePath(final String filename) {
        return buildFilePath(DCIM_CAMERA, filename);
    }

    /**
     * Generate a file path with the specified directory and filename
     *
     * @param dir
     *            The parent directory
     * @param filename
     *            The filename
     * @return the file path
     */
    public static String buildFilePath(final String dir, final String filename) {
        final File file = new File(dir, filename);
        file.getParentFile().mkdirs();
        return file.getPath();
    }

    /**
     * Add image into media store
     *
     * @param resolver
     *            The content resolver
     * @param title
     *            The image title
     * @param date
     *            The date time when image taken
     * @param location
     *            The location where image taken
     * @param orientation
     *            The orientation of image
     * @param size
     *            The image size in bytes
     * @param path
     *            The image file path
     * @param width
     *            The image width in pixel
     * @param height
     *            The image height in pixel
     * @return a content uri
     */
    public static Uri addImage(final ContentResolver resolver, final String title, final Date date, final Location location, final int orientation, final long size, final String path, final int width, final int height) {
        final ContentValues values = new ContentValues(9);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ".jpeg");
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date.getTime());
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        values.put(MediaStore.Images.ImageColumns.SIZE, size);

        if (HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(MediaStore.Images.ImageColumns.WIDTH, width);
            values.put(MediaStore.Images.ImageColumns.HEIGHT, height);
        }

        if (null != location) {
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location.getLongitude());
        }

        try {
            return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to add image into media store", t);
            return null;
        }
    }

    /**
     * Query media by the specified uri
     *
     * @param resolver
     *            The content resolver
     * @param uri
     *            The media uri
     * @return the media detail info
     */
    @SuppressLint("NewApi")
    public static ContentValues get(final ContentResolver resolver, final Uri uri) {
        final Cursor cursor = resolver.query(uri, null, null, null, null);
        if (null == cursor) {
            return null;
        }

        try {
            if (cursor.moveToNext()) {
                final int count = cursor.getColumnCount();
                final ContentValues values = new ContentValues();

                for (int i = 0; i < count; i++) {
                    final String name = cursor.getColumnName(i);

                    switch (cursor.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL:
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            values.put(name, cursor.getInt(i));
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            values.put(name, cursor.getFloat(i));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            values.put(name, cursor.getString(i));
                            break;
                        case Cursor.FIELD_TYPE_BLOB:
                            values.put(name, cursor.getBlob(i));
                            break;
                    }
                }

                return values;
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    /**
     * Delete media referenced by the specific uri
     *
     * @param resolver
     *            The content resolver
     * @param uri
     *            The content uri
     */
    public static void delete(final ContentResolver resolver, final Uri uri) {
        try {
            resolver.delete(uri, null, null);
        } catch (final Throwable t) {
            Log.e(TAG, "Failed to delete image from media store", t);
        }
    }

    /**
     * Write {@code data} into {@code path}
     *
     * @param data
     *            The binary data
     * @param path
     *            File path
     */
    public static void write(final byte[] data, final String path) {
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(path);
            out.write(data);
            out.flush();
        } catch (final Exception e) {
            Log.e(TAG, "Failed to write " + path, e);
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (final IOException e) {
                }
            }
        }
    }

    public static void writeFile(final String path, byte[] data) {
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(path);
            out.write(data);
            out.flush();
        } catch (final IOException e) {
            Log.e(TAG, "Failed to write file " + path, e);
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (final IOException e) {
                }
            }
        }
    }
}
