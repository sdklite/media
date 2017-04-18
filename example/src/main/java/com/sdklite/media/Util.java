package com.sdklite.media;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by johnson on 18/4/2017.
 */
public abstract class Util {

    private Util() {
    }

    public static Intent createImageChooser(final String title) {
        final Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
        getContent.setType("image/*");

        final Intent pickImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImage.setType("image/*");

        final Intent chooser = Intent.createChooser(getContent, title);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { pickImage });
        return chooser;
    }

    public static Bitmap resolveUriAsBitmap(final Context context, final Uri uri) {
        if (null == uri) {
            return null;
        }

        InputStream in = null;

        try {
            in = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(in);
        } catch (final FileNotFoundException e) {
            return null;
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (final IOException e) {
                }
            }
        }
    }

}
