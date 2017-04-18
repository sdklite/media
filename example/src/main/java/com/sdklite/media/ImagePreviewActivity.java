package com.sdklite.media;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.sdklite.media.example.R;
import com.sdklite.media.widget.CanvasView;

/**
 * Created by johnson on 18/4/2017.
 */
public class ImagePreviewActivity extends AppCompatActivity {

    private static final int REQ_SELECT_IMAGE = 1;

    private CanvasView mCanvasView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_image_preview);
        this.mCanvasView = (CanvasView) findViewById(R.id.activity_image_preview_canvas);
        this.startActivityForResult(Util.createImageChooser("Select Image"), REQ_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_SELECT_IMAGE: {
                if (RESULT_OK == resultCode) {
                    final Uri uri = data.getData();
                    if (null != uri) {
                        new AsyncTask<Uri, Void, Bitmap>() {
                            @Override
                            protected Bitmap doInBackground(final Uri... params) {
                                return Util.resolveUriAsBitmap(ImagePreviewActivity.this, params[0]);
                            }

                            @Override
                            protected void onPostExecute(final Bitmap bitmap) {
                                mCanvasView.setImageBitmap(bitmap);
                            }
                        }.execute(uri);
                    }
                }
                break;
            }
        }
    }
}
